package com.middleware.zeus.service.k8s;

/**
 * @author xutianhong
 * @Date 2023/8/3 3:48 下午
 */
public interface ClusterRoleService {

    /**
     * 初始化clusterRole
     * @param clusterId 集群id
     */
    void initClusterRole(String clusterId);

}
