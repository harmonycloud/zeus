package com.harmonycloud.zeus.service.dashboard;

import com.alibaba.fastjson.JSONArray;

/**
 * @author liyinlong
 * @since 2022/11/23 11:20 上午
 */
public interface RedisKVService {

    JSONArray getKeys(String clusterId, String namespace, String middlewareName, Integer db);

    JSONArray getKeysWithPattern(String clusterId, String namespace, String middlewareName, Integer db, String keyword);

    Integer dbSize(String clusterId, String namespace, String middlewareName, Integer db);
}
