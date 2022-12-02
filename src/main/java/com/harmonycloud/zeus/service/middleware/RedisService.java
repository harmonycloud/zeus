package com.harmonycloud.zeus.service.middleware;

import java.util.List;

import com.harmonycloud.caas.common.model.RedisDbDTO;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public interface RedisService {

    /**
     * 创建K-V
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param middlewareName 中间件名称
     * @param redisDbDTO 数据库对象
     */
    void create(String clusterId, String namespace, String middlewareName, RedisDbDTO redisDbDTO);

    /**
     * 修改K-V
     * 
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param middlewareName 中间件名称
     * @param redisDbDTO 数据库对象
     */
    void update(String clusterId, String namespace, String middlewareName, RedisDbDTO redisDbDTO);

    /**
     * 删除K-V
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param middlewareName 中间件名称
     * @param redisDbDTO 数据库对象
     */
    void delete(String clusterId, String namespace, String middlewareName, RedisDbDTO redisDbDTO);

    /**
     * 获取数据库集合
     * 
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param middlewareName 中间件名称
     * @param db 数据库
     * @param keyWord 关键词
     * @return
     */
    List<RedisDbDTO> listRedisDb(String clusterId, String namespace, String middlewareName, String db, String keyWord);

    /**
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param middlewareName 中间件名称
     * @param slaveName 从节点名称
     * @return
     */
    String getBurstMaster(String clusterId, String namespace, String middlewareName, String slaveName);
}
