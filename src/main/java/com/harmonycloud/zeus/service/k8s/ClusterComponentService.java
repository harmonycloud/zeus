package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;

/**
 * @author dengyulong
 * @date 2021/05/18
 */
public interface ClusterComponentService {

    /**
     * 部署组件
     *
     * @param cluster       集群
     * @param componentName 组件名称
     */
    void deploy(MiddlewareClusterDTO cluster, String componentName);

    /**
     * 对接组件
     *
     * @param cluster       集群
     * @param componentName 组件名称
     */
    void integrate(MiddlewareClusterDTO cluster, String componentName);

}
