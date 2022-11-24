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
import com.harmonycloud.zeus.integration.dashboard.RedisClient;
import com.harmonycloud.zeus.service.dashboard.RedisDashboardService;
import com.harmonycloud.zeus.service.dashboard.RedisKVService;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.util.K8sServiceNameUtil;
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
        String mod = getRedisMod(clusterId, namespace, middlewareName);
        // redis哨兵模式和单机模式的连接方式是一样的，因此连接方式只有集群模式和单机模式
        if (!"cluster".equals(mod)) {
            mod = "single";
        }

        String clusterAddrsPrefix = K8sServiceNameUtil.getServicePath(namespace, middlewareName) + ":" + port;
        String clusterAddrs = "test-redis-5."+clusterAddrsPrefix + "," + "test-redis-0."+clusterAddrsPrefix + ","+ "test-redis-1."+clusterAddrsPrefix;
        String sentinelAddrs = K8sServiceNameUtil.getServicePath(namespace, middlewareName) +":" +port;
        JSONObject res = redisClient.login(K8sServiceNameUtil.getServicePath(namespace, middlewareName), defaultDb, version,
                mod, username, password, port, clusterAddrs, sentinelAddrs);
        if (res.get("data") == null){
            throw new BusinessException(ErrorMessage.FAILED_TO_LOGIN_REDIS, res.getString("error"));
        }
        return res.getString("data");
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
            databaseDto.setSize(redisClient.DBSize(K8sServiceNameUtil.getServicePath(namespace, middlewareName), i).getInteger("data"));
            databaseDtoList.add(databaseDto);
        }
        return databaseDtoList;
    }

    @Override
    public DataDto getKeyValue(String clusterId, String namespace, String middlewareName, Integer db, String key) {
        JSONObject res = redisClient.getKeyValue(K8sServiceNameUtil.getServicePath(namespace, middlewareName), db, key);
        log.info(res.toJSONString());
        DataDto data = new DataDto(res.getJSONObject("data"));
        log.info(data.toString());
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
        redisClient.setKeyValue(K8sServiceNameUtil.getServicePath(namespace, middlewareName), db, key, keyValueDto);
    }

    @Override
    public void updateValue(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto) {
        keyValueDto.setValue(keyValueDto.wrapValue());
        // 更新value时，为避免expiration被重置，这里将expiration设置为空字符串
        keyValueDto.setExpiration("");
        redisClient.setKeyValue(K8sServiceNameUtil.getServicePath(namespace, middlewareName), db, key, keyValueDto);
    }

    @Override
    public void deleteKey(String clusterId, String namespace, String middlewareName, Integer db, String key) {
        redisClient.deleteKey(K8sServiceNameUtil.getServicePath(namespace, middlewareName), db, key);
    }

    @Override
    public void updateKey(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto) {
        if (!StringUtils.isEmpty(keyValueDto.getKey())) {
            redisClient.renameKey(K8sServiceNameUtil.getServicePath(namespace, middlewareName), db, key, keyValueDto.getKey());
        }
        if (!StringUtils.isEmpty(keyValueDto.getExpiration())) {
            // 添加时间单位：秒
            keyValueDto.setExpiration(keyValueDto.getExpiration() + "s");
            redisClient.setKeyExpiration(K8sServiceNameUtil.getServicePath(namespace, middlewareName), db, key, keyValueDto);
        }
    }

    @Override
    public void deleteValue(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto) {
        keyValueDto.setValue(keyValueDto.wrapValue());
        redisClient.removeValue(K8sServiceNameUtil.getServicePath(namespace, middlewareName), db, key, keyValueDto);
    }

    @Override
    public void setKeyExpiration(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto) {
        redisClient.setKeyExpiration(K8sServiceNameUtil.getServicePath(namespace, middlewareName), db, key, keyValueDto);
    }

    @Override
    public JSONObject execCMD(String clusterId, String namespace, String middlewareName, Integer db, String cmd) {
        JSONObject res = redisClient.execCMD(K8sServiceNameUtil.getServicePath(namespace, middlewareName), db, cmd);
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
            record.setStatus("false");
            res.put("success", false);
        } else {
            record.setStatus("true");
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

    /**
     * 获取redis集群模式
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @return
     */
    private String getRedisMod(String clusterId, String namespace, String middlewareName) {
        JSONObject installedValues = helmChartService.getInstalledValues(middlewareName, namespace, clusterService.findById(clusterId));
        return installedValues.getString("type");
    }

    private RedisKVService getKVService(String clusterId, String namespace, String middlewareName) {
        String type = getRedisMod(clusterId, namespace, middlewareName);
        return (RedisKVService) SpringContextUtils.getBean(getRedisServiceBeanName(type));
    }

    private String getRedisServiceBeanName(String type) {
        switch (type) {
            case "sentinel":
                return SentinelRedisKVServiceImpl.class.getSimpleName();
            case "cluster":
                return ClusterRedisKVServiceImpl.class.getSimpleName();
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