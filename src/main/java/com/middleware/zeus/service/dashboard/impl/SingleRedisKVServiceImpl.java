package com.middleware.zeus.service.dashboard.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.dashboard.redis.ScanResult;
import com.middleware.zeus.common.model.middleware.ServicePortDTO;
import com.middleware.zeus.integration.dashboard.RedisClient;
import com.middleware.zeus.service.dashboard.RedisKVService;
import com.middleware.zeus.service.k8s.ServiceService;
import com.middleware.zeus.util.K8sServiceNameUtil;
import com.middleware.zeus.util.middleware.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2022/11/23 11:25 上午
 */
@Service("SingleRedisKVServiceImpl")
public class SingleRedisKVServiceImpl implements RedisKVService {

    @Autowired
    private RedisClient redisClient;
    @Autowired
    private ServiceService serviceService;
    
    @Value("${system.middleware-api.redis.port:6379}")
    private String port;
    private static final Map<String,String> REDIS_PORT_MAP = new HashMap<>();

    public String getPort(String clusterId, String namespace, String middlewareName) {
        String middleware = clusterId + namespace + middlewareName;
        if (REDIS_PORT_MAP.containsKey(middleware)) {
            port = REDIS_PORT_MAP.get(middleware);
            return port;
        }
        ServicePortDTO servicePortDTO = serviceService.get(clusterId, namespace, middlewareName);
        if (servicePortDTO != null && !CollectionUtils.isEmpty(servicePortDTO.getPortDetailDtoList())) {
            port = servicePortDTO.getPortDetailDtoList().get(0).getPort();
            REDIS_PORT_MAP.put(middlewareName, port);
        }
        return port;
    }
    
    @Override
    public JSONArray getKeys(String clusterId, String namespace, String middlewareName, Integer db) {
        JSONObject res = redisClient.getAllKeys(K8sServiceNameUtil.getServicePath(namespace, middlewareName), getPort(clusterId, namespace, middlewareName), db);
        if (res.getJSONObject("err") != null) {
            throw new BusinessException(ErrorMessage.FAILED_TO_QUERY_KEY, res.getString("err"));
        }
        return res.getJSONArray("data");
    }

    @Override
    public JSONArray getKeysWithPattern(String clusterId, String namespace, String middlewareName, Integer db, String keyword) {
        JSONObject res = redisClient.getKeys(K8sServiceNameUtil.getServicePath(namespace, middlewareName), getPort(clusterId, namespace, middlewareName), db, keyword);
        if (res.getJSONObject("err") != null) {
            throw new BusinessException(ErrorMessage.FAILED_TO_QUERY_KEY, res.getString("err"));
        }
        return res.getJSONArray("data");
    }

    @Override
    public Integer dbSize(String clusterId, String namespace, String middlewareName, Integer db) {
        return redisClient.DBSize(K8sServiceNameUtil.getServicePath(namespace, middlewareName), getPort(clusterId, namespace, middlewareName), db).getInteger("data");
    }

    @Override
    public ScanResult scan(String clusterId, String namespace, String middlewareName, Integer db, String keyword, Integer cursor, Integer count, String shard) {
        JSONObject res = redisClient.scan(K8sServiceNameUtil.getServicePath(namespace, middlewareName), getPort(clusterId, namespace, middlewareName), db, keyword, cursor, count);
        return RedisUtil.convertScanResult(res);
    }

    @Override
    public JSONObject execCMD(String clusterId, String namespace, String middlewareName, Integer db, String cmd) {
        return redisClient.execCMD(K8sServiceNameUtil.getServicePath(namespace, middlewareName), getPort(clusterId, namespace, middlewareName), db, cmd);
    }

}
