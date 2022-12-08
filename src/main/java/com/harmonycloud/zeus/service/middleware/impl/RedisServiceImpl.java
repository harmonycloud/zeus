package com.harmonycloud.zeus.service.middleware.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.SENTINEL;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_EXPOSE_NODEPORT;
import static com.harmonycloud.zeus.util.RedisUtil.getRedisSentinelIsOk;

import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.model.Node;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRService;
import com.harmonycloud.zeus.service.k8s.NodeService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.harmonycloud.caas.common.constants.RedisConstant;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.RedisAccessInfo;
import com.harmonycloud.caas.common.model.RedisDbDTO;
import com.harmonycloud.zeus.operator.impl.RedisOperatorImpl;
import com.harmonycloud.zeus.service.k8s.IngressService;
import com.harmonycloud.zeus.service.k8s.impl.ServiceServiceImpl;
import com.harmonycloud.zeus.service.middleware.AbstractMiddlewareService;
import com.harmonycloud.zeus.service.middleware.RedisService;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Tuple;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Service
@Slf4j
public class RedisServiceImpl extends AbstractMiddlewareService implements RedisService {

    @Autowired
    private IngressService ingressService;
    @Autowired
    private MiddlewareServiceImpl middlewareService;
    @Autowired
    private ServiceServiceImpl serviceService;
    @Autowired
    private RedisOperatorImpl redisOperator;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private MiddlewareCRService middlewareCRService;

    @Override
    public List<RedisDbDTO> listRedisDb(String clusterId, String namespace, String middlewareName, String db,
        String keyWord) {
        paramCheck(db);
        RedisAccessInfo redisAccessInfo = checkAndGetDbManageAccessInfo(clusterId, namespace, middlewareName);
        Jedis jedis = getRedisSentinelIsOk(redisAccessInfo);
        List<RedisDbDTO> dbs;
        if (SENTINEL.equals(redisAccessInfo.getMode())) {
            if (RedisConstant.PONG.equalsIgnoreCase(jedis.ping())) {
                jedis.select(Integer.parseInt(db));
                dbs = convertDTO(jedis, Integer.parseInt(db), keyWord);
            } else {
                throw new BusinessException(ErrorMessage.REDIS_SERVER_CONNECT_FAILED);
            }
        } else {
            throw new BusinessException(ErrorMessage.TEMPORARY_NOT_SUPPORT_CLUSTER);
        }
        jedis.close();
        return dbs;
    }

    @Override
    public String getBurstMaster(String clusterId, String namespace, String middlewareName, String slaveName, String mode) {
        if (StringUtils.isBlank(slaveName)) {
            throw new BusinessException(ErrorMessage.REDIS_INCOMPLETE_PARAMETERS);
        }
        MiddlewareCR cr = middlewareCRService.getCR(clusterId, namespace, MiddlewareTypeEnum.REDIS.getType(), middlewareName);
        JSONObject status = JSONObject.parseObject(cr.getMetadata().getAnnotations().get("status"));
        JSONArray conditions = status.getJSONArray("conditions");
        if (CollectionUtil.isEmpty(conditions)){
            throw new BusinessException(DictEnum.POD,ErrorMessage.NOT_FOUND);
        }

        // 哨兵模式
        if ("sentinel".equals(mode)) {
            // 分片名字
            String burstName = slaveName.substring(0, slaveName.lastIndexOf("-"));
            for (Object condition : conditions) {
                JSONObject con = (JSONObject) condition;
                String conName = con.getString("name");
                if (conName.contains(burstName) && !slaveName.equals(conName)) {
                    return conName;
                }
            }
        } else {
            String masterNodeId = null;
            for (Object condition : conditions) {
                JSONObject con = (JSONObject) condition;
                if (slaveName.equals(con.getString("name"))) {
                    masterNodeId = con.getString("masterNodeId");
                    break;
                }
            }
            if (masterNodeId == null) throw new BusinessException(ErrorMessage.NODE_NOT_FOUND);
            for (Object condition : conditions) {
                JSONObject con = (JSONObject) condition;
                if (masterNodeId.equals(con.getString("nodeId"))) {
                    return con.getString("name");
                }
            }
        }
        throw new BusinessException(ErrorMessage.CANNOT_FIND_MASTER);
    }

    @Override
    public Map<String, String> burstList(String clusterId, String namespace, String middlewareName, String mode) {
        Map<String, String> burstMap = new HashMap<>();
        MiddlewareCR cr = middlewareCRService.getCR(clusterId, namespace, MiddlewareTypeEnum.REDIS.getType(), middlewareName);
        JSONObject status = JSONObject.parseObject(cr.getMetadata().getAnnotations().get("status"));
        if (status == null) {
            return null;
        }
        JSONArray conditions = status.getJSONArray("conditions");
        if (CollectionUtil.isEmpty(conditions)) {
            return null;
        }

        Map<String, JSONObject> conditionMap = new HashMap<>();
        // 哨兵模式
        if ("sentinel".equals(mode)) {
            conditions.forEach(condition -> {
                JSONObject con = (JSONObject) condition;
                if ("master".equals(con.getString("type"))) {
                    String name = con.getString("name");
                    String burstName = name.substring(0, name.lastIndexOf("-"));
                    conditionMap.put(burstName, con);
                }
            });
            conditions.forEach(condition -> {
                JSONObject con = (JSONObject) condition;
                if ("slave".equals(con.getString("type"))) {
                    String name = con.getString("name");
                    String burstName = name.substring(0, name.lastIndexOf("-"));
                    JSONObject master = conditionMap.get(burstName);
                    burstMap.put(master.getString("name"), con.getString("name"));
                }
            });
        } else {
            conditions.forEach(condition -> {
                JSONObject con = (JSONObject) condition;
                conditionMap.put(con.getString("nodeId"), con);
            });
            conditions.forEach(condition -> {
                JSONObject con = (JSONObject) condition;
                if ("slave".equals(con.getString("type"))) {
                    JSONObject masterNode = conditionMap.get(con.getString("masterNodeId"));
                    burstMap.put(masterNode.getString("name"), con.getString("name"));
                }
            });
        }
        return burstMap;

    }

    @Override
    public void create(String clusterId, String namespace, String middlewareName, RedisDbDTO db) {
        paramCheck(db.getDb());
        RedisAccessInfo redisAccessInfo = checkAndGetDbManageAccessInfo(clusterId, namespace, middlewareName);
        Jedis jedis;
        jedis = getRedisSentinelIsOk(redisAccessInfo);
        if (RedisConstant.PONG.equalsIgnoreCase(jedis.ping())) {
            jedis.select(Integer.parseInt(db.getDb()));
            if (RedisConstant.OUT.equals(db.getStatus())) {
                if (jedis.exists(db.getKey())) {
                    throw new BusinessException(ErrorMessage.KEY_ALREADY_EXISTS);
                }
            }
            paddingDataByType(jedis, db);
        } else {
            throw new BusinessException(ErrorMessage.REDIS_SERVER_CONNECT_FAILED);
        }
        jedis.close();
    }

    @Override
    public void update(String clusterId, String namespace, String middlewareName, RedisDbDTO db) {
        paramCheck(db.getDb());
        RedisAccessInfo redisAccessInfo = checkAndGetDbManageAccessInfo(clusterId, namespace, middlewareName);
        Jedis jedis;
        if (SENTINEL.equals(redisAccessInfo.getMode())) {
            jedis = getRedisSentinelIsOk(redisAccessInfo);
            if (RedisConstant.PONG.equalsIgnoreCase(jedis.ping())) {
                jedis.select(Integer.parseInt(db.getDb()));
                if (jedis.exists(db.getKey())) {
                    Long time = jedis.ttl(db.getKey());
                    if (db.getTimeOut() == null) {
                        db.setTimeOut(String.valueOf(time));
                    }
                    updateByType(jedis, db);
                }
            } else {
                throw new BusinessException(ErrorMessage.REDIS_SERVER_CONNECT_FAILED);
            }
        } else {
            throw new BusinessException(ErrorMessage.TEMPORARY_NOT_SUPPORT_CLUSTER);
        }
        jedis.close();
    }

    @Override
    public void delete(String clusterId, String namespace, String middlewareName, RedisDbDTO redisDbDTO) {
        paramCheck(redisDbDTO.getDb());
        RedisAccessInfo redisAccessInfo = checkAndGetDbManageAccessInfo(clusterId, namespace, middlewareName);
        Jedis jedis;
        if (SENTINEL.equals(redisAccessInfo.getMode())) {
            jedis = getRedisSentinelIsOk(redisAccessInfo);
            if (RedisConstant.PONG.equalsIgnoreCase(jedis.ping())) {
                jedis.select(Integer.parseInt(redisDbDTO.getDb()));
                if (jedis.exists(redisDbDTO.getKey())) {
                    removeByType(jedis, redisDbDTO);
                }
            } else {
                throw new BusinessException(ErrorMessage.REDIS_SERVER_CONNECT_FAILED);
            }
        } else {
            throw new BusinessException(ErrorMessage.TEMPORARY_NOT_SUPPORT_CLUSTER);
        }
        jedis.close();
    }

    /**
     * 修改时先删后增
     */
    private void updateByType(Jedis jedis, RedisDbDTO redisDbDTO) {
        long time = -1;
        if (StringUtils.isNotBlank(redisDbDTO.getTimeOut())) {
            if (redisDbDTO.getTimeOut().length() >= 16) {
                throw new BusinessException(ErrorMessage.OUT_OF_RANGE);
            }
            time = Long.parseLong(redisDbDTO.getTimeOut());
        }
        if (RedisConstant.LIST.equals(redisDbDTO.getType())) {
            for (String mapKey : redisDbDTO.getList().keySet()) {
                jedis.lset(redisDbDTO.getKey(), Long.parseLong(mapKey), redisDbDTO.getList().get(mapKey));
            }
        }
        if (RedisConstant.HASH.equals(redisDbDTO.getType())) {
            for (String mapKey : redisDbDTO.getOldHash().keySet()) {
                jedis.hdel(redisDbDTO.getKey(), mapKey);
            }
            jedis.hmset(redisDbDTO.getKey(), redisDbDTO.getHash());
        }
        if (RedisConstant.STRING.equals(redisDbDTO.getType())) {
            jedis.set(redisDbDTO.getKey(), redisDbDTO.getValue());
        }
        if (RedisConstant.SET.equals(redisDbDTO.getType())) {
            jedis.srem(redisDbDTO.getKey(), redisDbDTO.getOldSet());
            jedis.sadd(redisDbDTO.getKey(), redisDbDTO.getSet());
        }
        if (RedisConstant.Z_SET.equals(redisDbDTO.getType())) {
            for (String score : redisDbDTO.getZset().keySet()) {
                for (String mapKey : redisDbDTO.getOldZset().keySet()) {
                    jedis.zrem(redisDbDTO.getKey(), redisDbDTO.getOldZset().get(mapKey));
                }
                try {
                    jedis.zadd(redisDbDTO.getKey(), Double.parseDouble(score), redisDbDTO.getZset().get(score));
                } catch (Exception e) {
                    jedis.zadd(redisDbDTO.getKey(), 0, redisDbDTO.getZset().get(score));
                }
            }
        }
        if (time >= 0) {
            jedis.expireAt(redisDbDTO.getKey(), time);
        }
    }

    /**
     * 删除时分外部删除和内部删除
     */
    private void removeByType(Jedis jedis, RedisDbDTO redisDbDTO) {
        if (RedisConstant.LIST.equals(redisDbDTO.getType())) {
            if (MapUtils.isEmpty(redisDbDTO.getList())) {
                jedis.del(redisDbDTO.getKey());
            } else {
                for (String mapKey : redisDbDTO.getList().keySet()) {
                    jedis.lrem(redisDbDTO.getKey(), Long.parseLong(mapKey), redisDbDTO.getList().get(mapKey));
                }
            }
        }
        if (RedisConstant.HASH.equals(redisDbDTO.getType())) {
            if (MapUtils.isEmpty(redisDbDTO.getHash())) {
                jedis.del(redisDbDTO.getKey());
            } else {
                for (String mapKey : redisDbDTO.getHash().keySet()) {
                    jedis.hdel(redisDbDTO.getKey(), mapKey);
                }
            }
        }
        if (RedisConstant.STRING.equals(redisDbDTO.getType())) {
            jedis.del(redisDbDTO.getKey());
        }
        if (RedisConstant.SET.equals(redisDbDTO.getType())) {
            if (StringUtils.isEmpty(redisDbDTO.getSet())) {
                jedis.del(redisDbDTO.getKey());
            } else {
                jedis.srem(redisDbDTO.getKey(), redisDbDTO.getSet());
            }
        }
        if (RedisConstant.Z_SET.equals(redisDbDTO.getType())) {
            if (MapUtils.isEmpty(redisDbDTO.getZset())) {
                jedis.del(redisDbDTO.getKey());
            } else {
                for (String mapKey : redisDbDTO.getZset().keySet()) {
                    jedis.zrem(redisDbDTO.getKey(), redisDbDTO.getZset().get(mapKey));
                }
            }
        }
    }

    private List<RedisDbDTO> convertDTO(Jedis jedis, int db, String keyWord) {
        List<RedisDbDTO> dbs = new LinkedList<>();
        Set<String> keys = new HashSet<>();
        ScanParams scanParams = new ScanParams();
        if (StringUtils.isEmpty(keyWord)) {
            scanParams.match("*");
        } else {
            scanParams.match("*" + keyWord + "*");
        }
        scanParams.count(100000);// 每10万条查询
        String scanRet = "0";
        do {
            ScanResult<String> result = jedis.scan(scanRet, scanParams);
            scanRet = result.getCursor();// 返回用于下次遍历的游标
            keys.addAll(result.getResult());// 返回结果
        } while (!scanRet.equals("0"));
        for (String key : keys) {
            String type = jedis.type(key);
            RedisDbDTO redisDbDTO = new RedisDbDTO();
            String values = "";
            Map<String, String> hashs = new HashMap<>();
            List<String> lists = new ArrayList<>();
            Set<Tuple> zsets = new HashSet<>();
            Set<String> sets = new HashSet<>();
            if (RedisConstant.STRING.equals(type)) {
                values = jedis.get(key);
            }
            if (RedisConstant.HASH.equals(type)) {
                hashs = jedis.hgetAll(key);
            }
            if (RedisConstant.LIST.equals(type)) {
                lists = jedis.lrange(key, 0, -1);
            }
            if (RedisConstant.Z_SET.equals(type)) {
                zsets = jedis.zrangeWithScores(key, 0, -1);
            }
            if (RedisConstant.SET.equals(type)) {
                sets = jedis.smembers(key);
            }
            redisDbDTO.setHashs(hashs);
            redisDbDTO.setLists(lists);
            redisDbDTO.setSets(sets);
            redisDbDTO.setZsets(zsets);
            redisDbDTO.setValues(values);
            Long time = jedis.ttl(key);
            redisDbDTO.setKey(key);
            redisDbDTO.setType(type);
            redisDbDTO.setDb(String.valueOf(db));
            redisDbDTO.setTimeOut(String.valueOf(time));
            dbs.add(redisDbDTO);
        }
        return dbs;
    }

    private void paddingDataByType(Jedis jedis, RedisDbDTO redisDbDTO) {
        long time = -1;
        if (StringUtils.isNotBlank(redisDbDTO.getTimeOut())) {
            if (redisDbDTO.getTimeOut().length() >= 16) {
                throw new BusinessException(ErrorMessage.OUT_OF_RANGE);
            }
            time = Long.parseLong(redisDbDTO.getTimeOut());
        }
        if (!StringUtils.isEmpty(redisDbDTO.getType())) {
            switch (redisDbDTO.getType()) {
                case RedisConstant.STRING:
                    jedis.set(redisDbDTO.getKey(), redisDbDTO.getValue());
                    break;
                case RedisConstant.LIST:
                    for (String direction : redisDbDTO.getList().keySet()) {
                        if (RedisConstant.FRONT.equals(direction)) {
                            jedis.lpush(redisDbDTO.getKey(), redisDbDTO.getList().get(direction));
                        } else {
                            jedis.rpush(redisDbDTO.getKey(), redisDbDTO.getList().get(direction));
                        }
                    }
                    break;
                case RedisConstant.HASH:
                    jedis.hmset(redisDbDTO.getKey(), redisDbDTO.getHash());
                    break;
                case RedisConstant.SET:
                    jedis.sadd(redisDbDTO.getKey(), redisDbDTO.getSet());
                    break;
                case RedisConstant.Z_SET:
                    for (String score : redisDbDTO.getZset().keySet()) {
                        try {
                            jedis.zadd(redisDbDTO.getKey(), Double.parseDouble(score), redisDbDTO.getZset().get(score));
                        } catch (Exception e) {
                            jedis.zadd(redisDbDTO.getKey(), 0, redisDbDTO.getZset().get(score));
                        }
                    }
                    break;
                default:

            }
            if (time >= 0) {
                jedis.expireAt(redisDbDTO.getKey(), time);
            }
        }
    }

    /**
     * 如果没有暴露服务则手动暴露
     */
    private RedisAccessInfo checkAndGetDbManageAccessInfo(String clusterId, String namespace, String middlewareName) {
        RedisAccessInfo redisAccessInfo = queryBasicAccessInfo(clusterId, namespace, middlewareName);
        if (redisAccessInfo.isOpenService()) {
            return redisAccessInfo;
        } else {
            Middleware middleware = new Middleware();
            middleware.setClusterId(clusterId);
            middleware.setNamespace(namespace);
            middleware.setName(middlewareName);
            middleware.setType(MiddlewareTypeEnum.REDIS.getType());
            redisOperator.createOpenService(middleware);
            // ingress服务偶尔会有延迟，等待5秒
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return queryBasicAccessInfo(clusterId, namespace, middlewareName);
        }
    }

    /**
     * 获取redis暴露出来的地址
     */
    private RedisAccessInfo queryBasicAccessInfo(String clusterId, String namespace, String middlewareName) {
        Middleware middleware = middlewareService.detail(clusterId, namespace, middlewareName, MiddlewareTypeEnum.REDIS.getType());
        ReadWriteProxy readWriteProxy = middleware.getReadWriteProxy();
        if ("cluster".equals(middleware.getMode()) && !readWriteProxy.getEnabled()) {
            throw new BusinessException(ErrorMessage.TEMPORARY_NOT_SUPPORT_CLUSTER);
        }
        List<IngressDTO> serviceDTOS =
            ingressService.get(clusterId, namespace, MiddlewareTypeEnum.REDIS.getType(), middlewareName);

        RedisAccessInfo redisAccessInfo = new RedisAccessInfo();
        if (!CollectionUtils.isEmpty(serviceDTOS)) {
            // 优先使用ingress或NodePort暴露的服务
            List<IngressDTO> ingressDTOS = serviceDTOS.stream().filter(ingressDTO -> (!ingressDTO.getName().contains("readonly")))
                    .collect(Collectors.toList());
            IngressDTO ingressDTO = ingressDTOS.get(0);
            String exposeIP = "";
            if (StringUtils.equals(ingressDTO.getExposeType(), MIDDLEWARE_EXPOSE_NODEPORT)) {
                ingressDTO = serviceDTOS.get(0);
                List<Node> nodeList = nodeService.list(clusterId);
                if (!CollectionUtils.isEmpty(nodeList)) {
                    exposeIP = nodeList.get(0).getIp();
                }
            } else {
                exposeIP = ingressService.getIngressIp(clusterId, ingressDTO.getIngressClassName());
            }

            List<ServiceDTO> serviceList = ingressDTO.getServiceList();
            if (!CollectionUtils.isEmpty(serviceList)) {
                ServiceDTO serviceDTO = serviceList.get(0);
                String exposePort = serviceDTO.getExposePort();
                redisAccessInfo.setAddress(exposeIP + ":" + exposePort + " (集群外部)");
                redisAccessInfo.setHost(exposeIP);
                redisAccessInfo.setPort(exposePort);
            }
            redisAccessInfo.setOpenService(true);
        } else {
            // 没有暴露对外服务，则使用集群内服务
            ServicePortDTO servicePortDTO = serviceService.get(clusterId, namespace, middlewareName);
            if (servicePortDTO != null && !CollectionUtils.isEmpty(servicePortDTO.getPortDetailDtoList())) {
                redisAccessInfo.setAddress(servicePortDTO.getClusterIP() + ":"
                    + servicePortDTO.getPortDetailDtoList().get(0).getTargetPort() + "(集群内部)");
                redisAccessInfo.setHost(servicePortDTO.getClusterIP());
                redisAccessInfo.setPort(servicePortDTO.getPortDetailDtoList().get(0).getTargetPort());
            } else {
                redisAccessInfo.setAddress("无");
            }
            redisAccessInfo.setOpenService(false);
        }
        redisAccessInfo.setPassword(middleware.getPassword());
        redisAccessInfo.setMode(middleware.getMode());
        return redisAccessInfo;
    }

    public void paramCheck(String db) {
        if (StringUtils.isEmpty(db)) {
            throw new BusinessException(ErrorMessage.NOT_SELECT_DATABASE);
        }
    }

}
