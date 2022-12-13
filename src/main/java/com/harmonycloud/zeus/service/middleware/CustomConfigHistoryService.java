package com.harmonycloud.zeus.service.middleware;

import com.middleware.caas.common.model.middleware.MiddlewareCustomConfig;
import com.harmonycloud.zeus.bean.BeanCustomConfigHistory;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2022/3/4 3:41 下午
 */
public interface CustomConfigHistoryService {

    /**
     * 添加修改记录
     *
     * @param middlewareName    中间件名称
     * @param oldData           原数据
     * @param middlewareCustomConfig 新数据
     */
    void insert(String middlewareName, Map<String, String> oldData, MiddlewareCustomConfig middlewareCustomConfig);

    /**
     * 获取修改历史
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param name 中间件名称
     * @return List<BeanCustomConfigHistory>
     */
    List<BeanCustomConfigHistory> get(String clusterId, String namespace, String name);

    /**
     * 删除修改历史
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param name 中间件名称
     */
    void delete(String clusterId, String namespace, String name);

    /**
     * 更新修改历史
     *
     * @param beanCustomConfigHistory 数据对象
     */
    void update(BeanCustomConfigHistory beanCustomConfigHistory);

}
