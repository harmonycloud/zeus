package com.middleware.zeus.service.k8s;

import io.fabric8.kubernetes.api.model.rbac.RoleBinding;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/7/25 8:10 下午
 */
public interface RoleBindingService {

    /**
     * 绑定用户
     * @param clusterId 集群id
     * @param namespace 分区
     * @param name 名称
     * @param usernameList 用户名
     * @param clusterRole 集群角色
     */
    void bindUser(String clusterId, String namespace, String name, List<String> usernameList, String clusterRole);

    /**
     * 移除用户绑定关系
     * @param clusterId 集群id
     * @param namespace 分区
     * @param usernameList 用户名
     */
    void removeUser(String clusterId, String namespace, List<String> usernameList);

    /**
     * 绑定用户
     * @param clusterId 集群id
     * @param namespace 分区
     * @param name 名称
     * @param clusterRole 集群角色
     */
    void create(String clusterId, String namespace, String name, String clusterRole);

    /**
     * 删除roleBinding
     * @param clusterId 集群id
     * @param namespace 分区
     * @param name 名称
     * @param labels 标签
     */
    void delete(String clusterId, String namespace, String name, Map<String, String> labels);

}
