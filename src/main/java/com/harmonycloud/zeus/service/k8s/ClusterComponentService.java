package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.MultipleComponentsInstallDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareInfoDTO;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/05/18
 */
public interface ClusterComponentService {

     /** 部署组件
     *
     * @param cluster       集群对象
     * @param clusterComponentsDto 集群组件对象
     */
    void deploy(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto);

    /**
     * 批量部署组件
     *
     * @param cluster       集群对象
     * @param multipleComponentsInstallDto 集群组件批量安装对象
     */
    void multipleDeploy(MiddlewareClusterDTO cluster, MultipleComponentsInstallDto multipleComponentsInstallDto);

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
     * 删除集群绑定关系
     *
     * @param clusterId  集群id
     */
    void delete(String clusterId);

    /**
     * list所有组件
     *
     * @param clusterId 集群id
     * @return List<ClusterComponentsDto>
     */
    List<ClusterComponentsDto> list(String clusterId) throws Exception;

}
