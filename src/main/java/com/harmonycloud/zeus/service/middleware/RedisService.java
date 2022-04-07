package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.RedisDbDTO;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public interface RedisService {

    void create(String clusterId, String namespace, String middlewareName, RedisDbDTO redisDbDTO);

    void update(String clusterId, String namespace, String middlewareName, RedisDbDTO redisDbDTO);

    void delete(String clusterId, String namespace, String middlewareName, RedisDbDTO redisDbDTO);

    List<RedisDbDTO> listRedisDb(String clusterId, String namespace, String middlewareName, String db, String keyWord);

}
