package com.middleware.zeus.service.k8s;

/**
 * @author xutianhong
 * @Date 2023/8/2 4:59 下午
 */
public interface ClusterRoleBindingService {

    /**
     * 将用户添加至clusterRoleBinding
     * @param clusterId 集群id
     * @param name 名称
     * @param username 用户名称
     * @param clusterRole clusterRole名称
     */
    void addUserClusterRoleBinding(String clusterId, String name, String username, String clusterRole);

    /**
     * 将用户从clusterRoleBinding移除
     * @param clusterId 集群id
     * @param name 名称
     * @param username 用户名称
     */
    void removeUserClusterRoleBinding(String clusterId, String name, String username);

    /**
     * 创建clusterRoleBinding
     * @param clusterId 集群id
     * @param name 名称
     * @param clusterRole clusterRole名称
     */
    void create(String clusterId, String name, String clusterRole);

}
