package com.harmonycloud.zeus.service.middleware;

import com.middleware.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.bean.BeanCacheMiddleware;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/12/13 2:56 下午
 */
public interface CacheMiddlewareService {

    /**
     * 获取已删除中间件缓存信息列表
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param type      类型
     */
    List<BeanCacheMiddleware> list(String clusterId, String namespace, String type);

    /**
     * 获取已删除中间件缓存信息
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param type 中间件类型
     * @param name 中间件名称
     *
     * @return BeanCacheMiddleware
     */
    BeanCacheMiddleware get(String clusterId, String namespace, String type, String name);

    /**
     * 获取已删除中间件缓存信息
     *
     * @param middleware
     * @return BeanCacheMiddleware
     */
    BeanCacheMiddleware get(Middleware middleware);

    /**
     * 插入已删除中间件缓存信息
     *
     * @param beanCacheMiddleware
     */
    void insert(BeanCacheMiddleware beanCacheMiddleware);

    /**
     * 判断并保存中间件缓存信息
     * @param beanCacheMiddleware
     */
    void insertIfNotPresent(BeanCacheMiddleware beanCacheMiddleware);

    /**
     * 删除已删除中间件缓存信息
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param type 中间件类型
     * @param name 中间件名称
     */
    void delete(String clusterId, String namespace, String type, String name);


    /**
     * 删除已删除中间件缓存信息
     *
     * @param middleware
     */
    void delete(Middleware middleware);

    /**
     * 更新values.yaml为null
     *
     * @param middleware
     */
    void updateValuesToNull(Middleware middleware);

}
