package com.harmonycloud.zeus.service.k8s;

import java.util.List;
import java.util.Map;

import com.harmonycloud.caas.common.model.middleware.Namespace;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
public interface NamespaceService {

    /**
     * 查询命名空间列表
     *
     * @param clusterId 集群id
     * @return
     */
    List<Namespace> list(String clusterId);

    /**
     * 查询命名空间列表
     *
     * @param clusterId 集群id
     * @param all       是否返回所有命名空间
     * @param keyword   模糊搜索关键词
     * @return
     */
    List<Namespace> list(String clusterId, boolean all, String keyword);

    /**
     * 查询命名空间列表
     *
     * @param clusterId      集群id
     * @param all            是否返回所有命名空间
     * @param withQuota      是否返回配额
     * @param withMiddleware 是否返回中间件实例信息
     * @param keyword        模糊搜索关键词
     * @return
     */
    List<Namespace> list(String clusterId, boolean all, boolean withQuota, boolean withMiddleware, String keyword);

    /**
     * 查询命名空间列表
     *
     * @param clusterId     集群id
     * @param namespaceList 命名空间列表
     * @return List<String> 操作失败的命名空间
     */
    List<String> registry(String clusterId, List<String> namespaceList);

    /**
     * 创建命名空间
     *
     * @param clusterId     集群id
     * @param name          命名空间名称
     */
    void save(String clusterId, String name, Map<String, String> label);

}
