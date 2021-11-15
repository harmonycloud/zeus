package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareInfoDTO;

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
     * 批量部署组件
     *
     * @param cluster       集群对象
     * @param componentsDtoList 组建对象list
     * @param middlewareInfoDTOList operator对象list
     */
    void multipleDeploy(MiddlewareClusterDTO cluster, List<ClusterComponentsDto> componentsDtoList, List<MiddlewareInfoDTO> middlewareInfoDTOList);

    /**
     * 对接组件
     *
     * @param cluster       集群
     * @param componentName 组件名称
     */
    void integrate(MiddlewareClusterDTO cluster, String componentName);

    /**
     * 卸载/取消接入 组件
     *
     * @param cluster       集群对象
     * @param componentName 组件名称
     * @param status 部署类型:高可用，单实例
     */
    void delete(MiddlewareClusterDTO cluster, String componentName, Integer status);

    /**
     * list所有组件
     *
     * @param clusterId 集群id
     * @return List<ClusterComponentsDto>
     */
    List<ClusterComponentsDto> list(String clusterId);

}
