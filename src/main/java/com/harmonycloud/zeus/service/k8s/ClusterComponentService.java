package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/05/18
 */
public interface ClusterComponentService {

    /**
     * 部署组件
     *
     * @param cluster       集群对象
     * @param componentName 组件名称
     * @param type 部署类型:高可用，单实例
     */
    void deploy(MiddlewareClusterDTO cluster, String componentName, String type);

    /**
     * 对接组件
     *
     * @param cluster       集群
     * @param componentName 组件名称
     */
    void integrate(MiddlewareClusterDTO cluster, String componentName);

    /**
     * list所有组件
     *
     * @param clusterId 集群id
     * @return List<ClusterComponentsDto>
     */
    List<ClusterComponentsDto> list(String clusterId);

}
