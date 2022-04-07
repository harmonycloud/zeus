package com.harmonycloud.zeus.service.middleware.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.SENTINEL;

import com.harmonycloud.caas.common.constants.RedisConstant;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.RedisAccessInfo;
import com.harmonycloud.caas.common.model.RedisDbDTO;
import com.harmonycloud.caas.common.model.middleware.IngressDTO;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.ServiceDTO;
import com.harmonycloud.caas.common.model.middleware.ServicePortDTO;
import com.harmonycloud.zeus.service.k8s.IngressService;
import com.harmonycloud.zeus.service.k8s.impl.ServiceServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.harmonycloud.zeus.service.middleware.AbstractMiddlewareService;
import com.harmonycloud.zeus.service.middleware.RedisService;
import org.springframework.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import java.util.*;
import java.util.stream.Collectors;

import static com.harmonycloud.zeus.util.RedisUtil.getRedisClusterIsOk;
import static com.harmonycloud.zeus.util.RedisUtil.getRedisSentinelIsOk;

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

    @Override
    public List<RedisDbDTO> listRedisDb(String clusterId, String namespace, String middlewareName, String db, String keyWord) {
        RedisAccessInfo redisAccessInfo = queryBasicAccessInfo(clusterId,namespace,middlewareName,null);
        Jedis jedis = getRedisSentinelIsOk(redisAccessInfo);
        List<RedisDbDTO> dbs = new LinkedList<>();
        if (RedisConstant.PONG.equalsIgnoreCase(jedis.ping())) {
            if (SENTINEL.equals(redisAccessInfo.getMode())) {
                jedis.select(Integer.valueOf(db));
                dbs = convertDTO(jedis,Integer.valueOf(db),keyWord);
            } else {
                throw new BusinessException(ErrorMessage.TEMPORARY_NOT_SUPPORT_CLUSTER);
            }
        }
        jedis.close();
        return dbs;
    }

    @Override
    public void create(String clusterId, String namespace, String middlewareName, RedisDbDTO db) {
        RedisAccessInfo redisAccessInfo = queryBasicAccessInfo(clusterId,namespace,middlewareName,null);
        Jedis jedis = new Jedis();
        if (SENTINEL.equals(redisAccessInfo.getMode())) {
            jedis = getRedisSentinelIsOk(redisAccessInfo);
            if (RedisConstant.PONG.equalsIgnoreCase(jedis.ping())) {
                jedis.select(Integer.valueOf(db.getDb()));
                paddingDataByType(jedis,db);
            }
        } else {
            throw new BusinessException(ErrorMessage.TEMPORARY_NOT_SUPPORT_CLUSTER);
        }
        jedis.close();
    }

    @Override
    public void update(String clusterId, String namespace, String middlewareName, RedisDbDTO db) {
        RedisAccessInfo redisAccessInfo = queryBasicAccessInfo(clusterId,namespace,middlewareName,null);
        Jedis jedis = new Jedis();
        if (SENTINEL.equals(redisAccessInfo.getMode())) {
            jedis = getRedisSentinelIsOk(redisAccessInfo);
            if (RedisConstant.PONG.equalsIgnoreCase(jedis.ping())) {
                jedis.select(Integer.valueOf(db.getDb()));
                if (jedis.exists(db.getKey())) {
                    Long time = jedis.ttl(db.getKey());
                    if (db.getTimeOut() == null) {
                        db.setTimeOut(time);
                    }
                    updateByType(jedis,db);
                }
            }
        } else {
            throw new BusinessException(ErrorMessage.TEMPORARY_NOT_SUPPORT_CLUSTER);
        }
        jedis.close();
    }

    @Override
    public void delete(String clusterId, String namespace, String middlewareName, RedisDbDTO redisDbDTO) {
        RedisAccessInfo redisAccessInfo = queryBasicAccessInfo(clusterId,namespace,middlewareName,null);
        Jedis jedis = new Jedis();
        if (SENTINEL.equals(redisAccessInfo.getMode())) {
            jedis = getRedisSentinelIsOk(redisAccessInfo);
            if (RedisConstant.PONG.equalsIgnoreCase(jedis.ping())) {
                jedis.select(Integer.valueOf(redisDbDTO.getDb()));
                if (jedis.exists(redisDbDTO.getKey())) {
                    removeByType(jedis,redisDbDTO);
                }
            }
        } else {
            throw new BusinessException(ErrorMessage.TEMPORARY_NOT_SUPPORT_CLUSTER);
        }
        jedis.close();
    }

    /**
     * 修改时先删后增
     */
    public void updateByType(Jedis jedis, RedisDbDTO redisDbDTO) {
        if (RedisConstant.LIST.equals(redisDbDTO.getType())) {
            for (String mapKey : redisDbDTO.getList().keySet()) {
                jedis.lset(redisDbDTO.getKey(),Long.parseLong(mapKey),redisDbDTO.getList().get(mapKey));
            }
        }
        if (RedisConstant.HASH.equals(redisDbDTO.getType())) {
            for (String mapKey : redisDbDTO.getOldHash().keySet()) {
                jedis.hdel(redisDbDTO.getKey(),mapKey);
            }
            jedis.hmset(redisDbDTO.getKey(),redisDbDTO.getHash());
        }
        if (RedisConstant.STRING.equals(redisDbDTO.getType())) {
            jedis.set(redisDbDTO.getKey(),redisDbDTO.getValue());
        }
        if (RedisConstant.SET.equals(redisDbDTO.getType())) {
            jedis.srem(redisDbDTO.getKey(),redisDbDTO.getOldSet());
            jedis.sadd(redisDbDTO.getKey(),redisDbDTO.getSet());
        }
        if (RedisConstant.Z_SET.equals(redisDbDTO.getType())) {
            for (String score : redisDbDTO.getZset().keySet()) {
                for (String mapKey : redisDbDTO.getOldZset().keySet()) {
                    jedis.zrem(redisDbDTO.getKey(),redisDbDTO.getOldZset().get(mapKey));
                }
                try {
                    jedis.zadd(redisDbDTO.getKey(), Double.parseDouble(score),redisDbDTO.getZset().get(score));
                } catch (Exception e) {
                    jedis.zadd(redisDbDTO.getKey(), 0,redisDbDTO.getZset().get(score));
                }
            }
        }
    }

    /**
     * 删除时分外部删除和内部删除
     */
    public void removeByType(Jedis jedis, RedisDbDTO redisDbDTO) {
        if (RedisConstant.LIST.equals(redisDbDTO.getType())) {
            if (MapUtils.isEmpty(redisDbDTO.getList())) {
                jedis.del(redisDbDTO.getKey());
            } else {
                for (String mapKey : redisDbDTO.getList().keySet()) {
                    jedis.lrem(redisDbDTO.getKey(),Long.parseLong(mapKey),redisDbDTO.getList().get(mapKey));
                }
            }
        }
        if (RedisConstant.HASH.equals(redisDbDTO.getType())) {
            if (MapUtils.isEmpty(redisDbDTO.getHash())) {
                jedis.del(redisDbDTO.getKey());
            } else {
                for (String mapKey : redisDbDTO.getHash().keySet()) {
                    jedis.hdel(redisDbDTO.getKey(),mapKey);
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
                jedis.srem(redisDbDTO.getKey(),redisDbDTO.getSet());
            }
        }
        if (RedisConstant.Z_SET.equals(redisDbDTO.getType())) {
            if (MapUtils.isEmpty(redisDbDTO.getZset())) {
                jedis.del(redisDbDTO.getKey());
            } else {
                for (String mapKey : redisDbDTO.getZset().keySet()) {
                    jedis.zrem(redisDbDTO.getKey(),redisDbDTO.getZset().get(mapKey));
                }
            }
        }
    }

    public List<RedisDbDTO> convertDTO(Jedis jedis, int db, String keyWord) {
        List<RedisDbDTO> dbs = new LinkedList<>();
        Set<String> keys;
        if (StringUtils.isEmpty(keyWord)) {
             keys = jedis.keys("*");
        } else {
            keys = jedis.keys(keyWord + "*");
        }
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
                lists = jedis.lrange(key,0,-1);
            }
            if (RedisConstant.Z_SET.equals(type)) {
                zsets = jedis.zrangeWithScores(key,0,-1);
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
            redisDbDTO.setTimeOut(time);
            dbs.add(redisDbDTO);
        }
        return dbs;
    }

    public void paddingDataByType(Jedis jedis, RedisDbDTO redisDbDTO) {
        if (!StringUtils.isEmpty(redisDbDTO.getType())) {
            switch (redisDbDTO.getType()) {
                case RedisConstant.STRING:
                    jedis.set(redisDbDTO.getKey(),redisDbDTO.getValue());
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
                    jedis.hmset(redisDbDTO.getKey(),redisDbDTO.getHash());
                    break;
                case RedisConstant.SET:
                    jedis.sadd(redisDbDTO.getKey(),redisDbDTO.getSet());
                    break;
                case RedisConstant.Z_SET:
                    for (String score : redisDbDTO.getZset().keySet()) {
                        try {
                            jedis.zadd(redisDbDTO.getKey(), Double.parseDouble(score),redisDbDTO.getZset().get(score));
                        } catch (Exception e) {
                            jedis.zadd(redisDbDTO.getKey(), 0,redisDbDTO.getZset().get(score));
                        }
                    }
                    break;
                default:

            }
            if (redisDbDTO.getTimeOut() != null && redisDbDTO.getTimeOut().intValue() != -1 ) {
                jedis.expire(redisDbDTO.getKey(),redisDbDTO.getTimeOut().intValue());
            }
        }
    }

    /**
     * 获取redis暴露出来的地址
     */
    public RedisAccessInfo queryBasicAccessInfo(String clusterId, String namespace, String middlewareName, Middleware middleware) {
        if (middleware == null) {
            middleware = middlewareService.detail(clusterId, namespace, middlewareName, MiddlewareTypeEnum.REDIS.getType());
        }
        List<IngressDTO> ingressDTOS = ingressService.get(clusterId, namespace, MiddlewareTypeEnum.REDIS.name(), middlewareName);
        ingressDTOS = ingressDTOS.stream().filter(ingressDTO -> (
                !ingressDTO.getName().contains("readonly"))
        ).collect(Collectors.toList());

        RedisAccessInfo redisAccessInfo = new RedisAccessInfo();
        if (!CollectionUtils.isEmpty(ingressDTOS)) {
            // 优先使用ingress或NodePort暴露的服务
            IngressDTO ingressDTO = ingressDTOS.get(0);
            String exposeIP = ingressDTO.getExposeIP();
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
                redisAccessInfo.setAddress(servicePortDTO.getClusterIP() + ":" + servicePortDTO.getPortDetailDtoList().get(0).getTargetPort() + "(集群内部)");
                redisAccessInfo.setHost(servicePortDTO.getClusterIP());
                redisAccessInfo.setPort(servicePortDTO.getPortDetailDtoList().get(0).getTargetPort());
                Map<String, Integer> map = new HashMap<>();
            } else {
                redisAccessInfo.setAddress("无");
            }
            redisAccessInfo.setOpenService(false);
        }
        redisAccessInfo.setPassword(middleware.getPassword());
        redisAccessInfo.setMode(middleware.getMode());
        return redisAccessInfo;
    }

    public void paramCheck(String clusterId, String namespace, String middlewareName) {
        if (StringUtils.isAnyEmpty(clusterId,namespace,middlewareName)) {
            throw new BusinessException(ErrorMessage.REDIS_INCOMPLETE_PARAMETERS);
        }
    }

}
