package com.middleware.zeus.service.dashboard;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.model.dashboard.redis.DataDto;
import com.middleware.caas.common.model.dashboard.redis.DatabaseDto;
import com.middleware.caas.common.model.dashboard.redis.KeyValueDto;
import com.middleware.caas.common.model.dashboard.redis.ScanResult;

import java.util.List;

/**
 * @author liyinlong
 * @since 2022/10/27 10:41 上午
 */
public interface RedisDashboardService extends BaseMiddlewareApiService{

    List<KeyValueDto> getAllKeys(String clusterId, String namespace, String middlewareName, Integer db, String keyword);

    ScanResult scan(String clusterId, String namespace, String middlewareName, Integer db, String keyword, Integer cursor, Integer count, String pod, Integer preCursor, String prePod);

    List<DatabaseDto> getDBList(String clusterId, String namespace, String middlewareName);

    DataDto getKeyValue(String clusterId, String namespace, String middlewareName, Integer db, String key);

    void setKeyValue(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto);

    void updateValue(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto);

    void deleteKey(String clusterId, String namespace, String middlewareName, Integer db, String key);

    void renameKey(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto);

    void deleteValue(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto);

    void expireKey(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto);

    JSONObject execCMD(String clusterId, String namespace, String middlewareName, Integer db, String cmd);

}
