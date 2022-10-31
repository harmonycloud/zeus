package com.harmonycloud.zeus.service.dashboard.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.dashboard.ExecuteSqlDto;
import com.harmonycloud.caas.common.model.dashboard.redis.DataDto;
import com.harmonycloud.caas.common.model.dashboard.redis.DatabaseDto;
import com.harmonycloud.caas.common.model.dashboard.redis.KeyValueDto;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.integration.dashboard.RedisClient;
import com.harmonycloud.zeus.service.dashboard.RedisDashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liyinlong
 * @since 2022/10/27 10:44 上午
 */
@Slf4j
@Service
@Operator(paramTypes4One = String.class)
public class RedisDashboardServiceImpl implements RedisDashboardService {

    @Autowired
    private RedisClient redisClient;

    private String port = "6379";

    @Override
    public boolean support(String type) {
        return false;
    }

    @Override
    public String login(String clusterId, String namespace, String middlewareName, String username, String password) {
        return null;
    }

    @Override
    public List<KeyValueDto> getAllKeys(String clusterId, String namespace, String middlewareName, Integer db, String keyword) {
        return convertToKeyValueDto(redisClient.getAllKeys(getPath(namespace, middlewareName), db));
    }

    @Override
    public List<DatabaseDto> getDBList(String clusterId, String namespace, String middlewareName) {
        List<DatabaseDto> databaseDtoList = new ArrayList<>();
        // todo 数据库并非都是 16个
        for (int i = 0; i < 16; i++) {
            DatabaseDto databaseDto = new DatabaseDto();
            databaseDto.setDb(i);
            databaseDto.setSize(redisClient.DBSize(getPath(namespace, middlewareName), i).getInteger("data"));
            databaseDtoList.add(databaseDto);
        }
        return databaseDtoList;
    }

    @Override
    public DataDto getKeyValue(String clusterId, String namespace, String middlewareName, Integer db, String key) {
        JSONObject res = redisClient.getKeyValue(getPath(namespace, middlewareName), db, key);
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
        redisClient.setKeyValue(getPath(namespace, middlewareName), db, key, keyValueDto);
    }

    @Override
    public void deleteKey(String clusterId, String namespace, String middlewareName, Integer db, String key) {
        redisClient.deleteKey(getPath(namespace, middlewareName), db, key);
    }

    @Override
    public void updateKey(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto) {
        if (!StringUtils.isEmpty(keyValueDto.getKey())) {
            redisClient.renameKey(getPath(namespace, middlewareName), db, key, keyValueDto.getKey());
        }
        if (!StringUtils.isEmpty(keyValueDto.getExpiration())) {
            redisClient.setKeyExpiration(getPath(namespace, middlewareName), db, key, keyValueDto);
        }
    }

    @Override
    public void deleteValue(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto) {
        keyValueDto.setValue(keyValueDto.wrapValue());
        redisClient.removeValue(getPath(namespace, middlewareName), db, key, keyValueDto);
    }

    @Override
    public void setKeyExpiration(String clusterId, String namespace, String middlewareName, Integer db, String key, KeyValueDto keyValueDto) {
        redisClient.setKeyExpiration(getPath(namespace, middlewareName), db, key, keyValueDto);
    }

    @Override
    public JSONObject execCMD(String clusterId, String namespace, String middlewareName, Integer db, String cmd) {
        return null;
    }

    @Override
    public List<ExecuteSqlDto> listExecuteSql(String clusterId, String namespace, String middlewareName, Integer db, String keyword,String start,String end) {
        return null;
    }

    private List<KeyValueDto> convertToKeyValueDto(JSONObject object) {
        if (object.getJSONObject("err") != null) {
            // throws exception
        }
        JSONArray dataAry = object.getJSONArray("data");
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

    private String getPath(String namespace, String middlewareName){
        return "10.10.102.52";
    }

}
