package com.middleware.zeus.service.dashboard;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.model.dashboard.redis.ScanResult;

/**
 * @author liyinlong
 * @since 2022/11/23 11:20 上午
 */
public interface RedisKVService {

    JSONArray getKeys(String clusterId, String namespace, String middlewareName, Integer db);

    JSONArray getKeysWithPattern(String clusterId, String namespace, String middlewareName, Integer db, String keyword);

    Integer dbSize(String clusterId, String namespace, String middlewareName, Integer db);

    ScanResult scan(String clusterId, String namespace, String middlewareName, Integer db, String keyword, Integer cursor, Integer count, String shard);

    JSONObject execCMD(String clusterId, String namespace, String middlewareName, Integer db, String cmd);

}
