package com.harmonycloud.zeus.service.dashboard.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.dashboard.redis.ScanResult;
import com.harmonycloud.zeus.integration.dashboard.RedisClient;
import com.harmonycloud.zeus.service.dashboard.RedisKVService;
import com.harmonycloud.zeus.util.K8sServiceNameUtil;
import com.harmonycloud.zeus.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2022/11/23 11:25 上午
 */
@Service("SingleRedisKVServiceImpl")
public class SingleRedisKVServiceImpl implements RedisKVService {

    @Autowired
    private RedisClient redisClient;

    @Value("${system.middleware-api.redis.port:6379}")
    private String port;

    @Override
    public JSONArray getKeys(String clusterId, String namespace, String middlewareName, Integer db) {
        JSONObject res = redisClient.getAllKeys(K8sServiceNameUtil.getServicePath(namespace, middlewareName), port, db);
        if (res.getJSONObject("err") != null) {
            throw new BusinessException(ErrorMessage.FAILED_TO_QUERY_KEY, res.getString("err"));
        }
        return res.getJSONArray("data");
    }

    @Override
    public JSONArray getKeysWithPattern(String clusterId, String namespace, String middlewareName, Integer db, String keyword) {
        JSONObject res = redisClient.getKeys(K8sServiceNameUtil.getServicePath(namespace, middlewareName), port, db, keyword);
        if (res.getJSONObject("err") != null) {
            throw new BusinessException(ErrorMessage.FAILED_TO_QUERY_KEY, res.getString("err"));
        }
        return res.getJSONArray("data");
    }

    @Override
    public Integer dbSize(String clusterId, String namespace, String middlewareName, Integer db) {
        return redisClient.DBSize(K8sServiceNameUtil.getServicePath(namespace, middlewareName), port, db).getInteger("data");
    }

    @Override
    public ScanResult scan(String clusterId, String namespace, String middlewareName, Integer db, String keyword, Integer cursor, Integer count, String shard) {
        JSONObject res = redisClient.scan(K8sServiceNameUtil.getServicePath(namespace, middlewareName), port, db, keyword, cursor, count);
        return RedisUtil.convertScanResult(res);
    }

}
