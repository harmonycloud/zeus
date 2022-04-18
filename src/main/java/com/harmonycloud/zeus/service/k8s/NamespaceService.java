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
     * @param projectId      项目id
     * @return
     */
    List<Namespace> list(String clusterId, boolean all, boolean withQuota, boolean withMiddleware, String keyword, String projectId);

    /**
     * 查询命名空间列表
     *
     * @param clusterId     集群id
     * @param namespaceList 命名空间列表
     * @return List<String> 操作失败的命名空间
     */
    //List<String> registry(String clusterId, List<String> namespaceList);

    /**
     * 创建命名空间
     *
     * @param namespace 分区对象
     * @param label         标签
     * @param exist    是否校验分区同名
     */
    void save(Namespace namespace, Map<String, String> label, Boolean exist);

    /**
     * 创建命名空间
     *
     * @param clusterId     集群id
     * @param name          命名空间名称
     * @param label         label
     * @param annotations   annotations
     */
    void save(String clusterId, String name, Map<String, String> label, Map<String, String> annotations);

    /**
     * 删除命名空间
     *
     * @param clusterId     集群id
     * @param name          命名空间名称
     */
    void delete(String clusterId, String name);

    /**
     *
     * @param clusterId 集群id
     * @param name 分区名称
     * @param registered 是否注册
     */
    void registry(String clusterId, String name, Boolean registered);

}
