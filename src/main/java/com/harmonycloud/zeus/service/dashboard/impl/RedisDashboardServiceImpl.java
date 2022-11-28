package com.harmonycloud.zeus.service.dashboard.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.dashboard.redis.DataDto;
import com.harmonycloud.caas.common.model.dashboard.redis.DatabaseDto;
import com.harmonycloud.caas.common.model.dashboard.redis.KeyValueDto;
import com.harmonycloud.caas.common.model.dashboard.redis.ZSetDto;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.bean.BeanSqlExecuteRecord;
import com.harmonycloud.zeus.dao.BeanSqlExecuteRecordMapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.integration.dashboard.RedisClient;
import com.harmonycloud.zeus.service.dashboard.RedisDashboardService;
import com.harmonycloud.zeus.service.dashboard.RedisKVService;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.util.K8sServiceNameUtil;
import com.harmonycloud.zeus.util.RedisUtil;
import com.harmonycloud.zeus.util.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2022/10/27 10:44 上午
 */
@Slf4j
@Service
@Operator(paramTypes4One = String.class)
public class RedisDashboardServiceImpl implements RedisDashboardService {

    @Value("${system.middleware-api.redis.port:6379}")
    private String port;

    @Autowired
    private HelmChartService  helmChartService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private BeanSqlExecuteRecordMapper sqlExecuteRecordMapper;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private MiddlewareService middlewareService;

    @Override
    public boolean support(String type) {
        return MiddlewareTypeEnum.REDIS.getType().equals(type);
    }

    @Override
    public String login(String clusterId, String namespace, String middlewareName, String username, String password) {
        JSONObject installedValues = helmChartService.getInstalledValues(middlewareName, namespace, clusterService.findById(clusterId));
        String tempVersion = installedValues.getString("version");
        String version = "5";
        if (!StringUtils.isEmpty(tempVersion)) {
            version = tempVersion.split("\\.")[0];
        }
        if (Integer.parseInt(version) < 6) {
            username = "";
        }
        int defaultDb = 0;
        String deployMod = RedisUtil.getRedisDeployMod(installedValues);
        // redis哨兵模式和单机模式的连接方式是一样的，因此连接方式只有集群模式和单机模式
        String redisPort = port;
        String clusterAddrs = "";
        String sentinelAddrs = "";
        String redisMod = "";
        String host = K8sServiceNameUtil.getServicePath(namespace, middlewareName);
        switch (deployMod) {
            case "cluster":
            case "clusterProxy":
                redisMod = "cluster";
                clusterAddrs = getClusterAddress(clusterId, namespace, middlewareName);
                break;
            case "sentinel":
                redisMod = "single";
                sentinelAddrs = K8sServiceNameUtil.getServicePath(namespace, middlewareName) + ":" + port;
                break;
            case "sentinelProxy":
                redisMod = "single";
                redisPort = "7617";
                host = K8sServiceNameUtil.getRedisPredixyServicePath(namespace, middlewareName);
                break;
            default:
                throw new BusinessException(ErrorMessage.UNKNOWN_REDIS_CLUSTER);
        }
        JSONObject res = redisClient.login(defaultDb, host, version,
                redisMod, username, password, redisPort, clusterAddrs, sentinelAddrs);
        if (res.get("data") == null) {
            throw new BusinessException(ErrorMessage.FAILED_TO_LOGIN_REDIS, res.getString("error"));
        }
        return res.getString("data");
    }

    @Override
    public void logout(String clusterId, String namespace, String middlewareName) {

    }

    @Override
    public List<KeyValueDto> getAllKeys(String clusterId, String namespace, String middlewareName, Integer db, String keyword) {
        RedisKVService redisKVService = getKVService(clusterId, namespace, middlewareName);
        if (StringUtils.isEmpty(keyword)) {
            return convertToKeyValueDto(redisKVService.getKeys(clusterId, namespace, middlewareName, db));
        } else {
            keyword = "*" + keyword + "*";
            return convertToKeyValueDto(redisKVService.getKeysWithPattern(clusterId, namespace, middlewareName, db, keyword));
        }
    }

    @Override
    public List<DatabaseDto> getDBList(String clusterId, String namespace, String middlewareName) {
        List<DatabaseDto> databaseDtoList = new ArrayList<>();
        int dbNum = getDBNum(clusterId, namespace, middlewareName);
        for (int i = 0; i < dbNum; i++) {
            DatabaseDto databaseDto = new DatabaseDto();
            databaseDto.setDb(i);
            databaseDto.setSize(redisClient.DBSize(i).getInteger("data"));
            databaseDtoList.add(databaseDto);
        }
        return databaseDtoList;
    }

    @Override
    public DataDto getKeyValue(String clusterId, String namespace, String middlewareName, Integer db, String key) {
        JSONObject res = redisClient.getKeyValue(db, key);
        DataDto data = new DataDto(res.getJSONObject("data"));
        log.debug(data.toString());
        return data;
    }

    @Override
    public void setKeyValue(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto) {
        keyValueDto.setValue(keyValueDto.wrapValue());
        // 给过期时间添加时间单位：秒
        if (!StringUtils.isEmpty(keyValueDto.getExpiration())) {
            keyValueDto.setExpiration(keyValueDto.getExpiration() + "s");
        }
        // 如果zset数据类型没有值，则添加一个默认值
        if ("zset".equals(keyValueDto.getKeyType()) && keyValueDto.getZsetValue() == null) {
            ZSetDto zSetDto = new ZSetDto();
            zSetDto.setMember("default");
            zSetDto.setScore("1");
            keyValueDto.setZsetValue(zSetDto);
            keyValueDto.setValue(keyValueDto.wrapValue());
        }
        redisClient.setKeyValue(db, key, keyValueDto);
    }

    @Override
    public void updateValue(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto) {
        keyValueDto.setValue(keyValueDto.wrapValue());
        // 更新value时，为避免expiration被重置，这里将expiration设置为空字符串
        keyValueDto.setExpiration("");
        redisClient.setKeyValue(db, key, keyValueDto);
    }

    @Override
    public void deleteKey(String clusterId, String namespace, String middlewareName, Integer db, String key) {
        redisClient.deleteKey(db, key);
    }

    @Override
    public void renameKey(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto) {
        if (!StringUtils.isEmpty(keyValueDto.getKey())) {
            redisClient.renameKey(db, key, keyValueDto.getKey());
        }
    }

    @Override
    public void deleteValue(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto) {
        keyValueDto.setValue(keyValueDto.wrapValue());
        redisClient.removeValue(db, key, keyValueDto);
    }

    @Override
    public void expireKey(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto) {
        if (!StringUtils.isEmpty(keyValueDto.getExpiration())) {
            // 添加时间单位：秒
            keyValueDto.setExpiration(keyValueDto.getExpiration() + "s");
            redisClient.setKeyExpiration(db, key, keyValueDto);
        }
    }

    @Override
    public JSONObject execCMD(String clusterId, String namespace, String middlewareName, Integer db, String cmd) {
        JSONObject res = redisClient.execCMD(db, cmd);
        BeanSqlExecuteRecord record = new BeanSqlExecuteRecord();
        String err = res.getString("err");
        record.setClusterId(clusterId);
        record.setNamespace(namespace);
        record.setMiddlewareName(middlewareName);
        record.setTargetDatabase(String.valueOf(db));
        // 设置sql
        String[] cmds = cmd.split(" ");
        if (cmds.length > 0) {
            record.setSqlStr(cmds[0]);
        } else {
            record.setSqlStr(cmd);
        }
        //  设置执行状态和提示信息
        if (!StringUtils.isEmpty(err)) {
            record.setMessage(err);
            record.setExecStatus("false");
            res.put("success", false);
        } else {
            record.setExecStatus("true");
            // 设置命令执行时长
            record.setExecTime(res.getString("execTime"));
            res.put("success", true);
        }
        // 设置命令执行时间
        record.setExecDate(new Date());
        sqlExecuteRecordMapper.insert(record);
        return res;
    }

    /**
     * 获取redis集群连接地址
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @return
     */
    private String getClusterAddress(String clusterId, String namespace, String middlewareName) {
        String clusterAddrsPrefix = K8sServiceNameUtil.getServicePath(namespace, middlewareName) + ":" + port;
        List<MiddlewareInfo> pods = listMasterPod(clusterId, namespace, middlewareName);
        StringBuilder sbf = new StringBuilder();
        for (int i = 0; i < pods.size(); i++) {
            sbf.append(pods.get(i).getName());
            sbf.append(".");
            sbf.append(clusterAddrsPrefix);
            if (i != (pods.size() - 1)) {
                sbf.append(",");
            }
        }
        if (StringUtils.isEmpty(sbf.toString())) {
            sbf.append(clusterAddrsPrefix);
        }
        return sbf.toString();
    }

    private List<MiddlewareInfo> listMasterPod(String clusterId, String namespace, String middlewareName) {
        List<MiddlewareInfo> pods = middlewareService.listMiddlewarePod(clusterId, namespace, MiddlewareTypeEnum.REDIS.getType(), middlewareName);
        return pods.stream().filter(middlewareInfo -> middlewareInfo.getType() != null && middlewareInfo.getType().equals("master")).collect(Collectors.toList()).
                stream().sorted(new RedisAggregationKVServiceImpl.MiddlewareInfoComparator()).collect(Collectors.toList());
    }

    /**
     * 获取数据库数量，集群模式只有1个库，其他模式有16个库
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @return
     */
    private int getDBNum(String clusterId, String namespace, String middlewareName) {
        JSONObject installedValues = helmChartService.getInstalledValues(middlewareName, namespace, clusterService.findById(clusterId));
        String type = installedValues.getString("type");
        int dbNum = 16;
        if (type.equals("cluster")) {
            dbNum = 1;
        }
        return dbNum;
    }

    private RedisKVService getKVService(String clusterId, String namespace, String middlewareName) {
        JSONObject installedValues = helmChartService.getInstalledValues(middlewareName, namespace, clusterService.findById(clusterId));
        String deployMod = RedisUtil.getRedisDeployMod(installedValues);
        return (RedisKVService) SpringContextUtils.getBean(getRedisServiceBeanName(deployMod));
    }

    private String getRedisServiceBeanName(String deployMod) {
        switch (deployMod) {
            case "sentinel":
                return SentinelRedisKVServiceImpl.class.getSimpleName();
            case "cluster":
            case "clusterProxy":
            case "sentinelProxy":
                return RedisAggregationKVServiceImpl.class.getSimpleName();
            default:
                throw new BusinessException(ErrorMessage.UNKNOWN_REDIS_CLUSTER);
        }
    }

    private List<KeyValueDto> convertToKeyValueDto(JSONArray dataAry) {
        if (CollectionUtils.isEmpty(dataAry)) {
            return Collections.emptyList();
        }
        List<KeyValueDto> resList = new ArrayList<>();
        dataAry.forEach(data -> {
            JSONObject obj = (JSONObject) data;
            KeyValueDto keyValueDto = new KeyValueDto();
            keyValueDto.setKey(obj.getString("key"));
            keyValueDto.setKeyType(obj.getString("type"));
            resList.add(keyValueDto);
        });
        return resList;
    }

}
