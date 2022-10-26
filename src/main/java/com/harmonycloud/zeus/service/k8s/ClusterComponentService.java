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
    void multipleDeploy(MiddlewareClusterDTO cluster, MultipleComponentsInstallDto multipleComponentsInstallDto) throws Exception;

    /**
     * 接入/更新组件
     *
     * @param clusterComponentsDto 组件对象
     * @param update 是否为更新
     */
    void integrate(ClusterComponentsDto clusterComponentsDto, Boolean update);

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

    /**
     * 获取指定组件
     *
     * @param clusterId 集群id
     * @param component 组件
     * @return ClusterComponentsDto
     */
    ClusterComponentsDto get(String clusterId, String component);

    /**
     *
     * 检查组件是否已安装
     * @param clusterId
     * @param componentName
     * @return
     */
    boolean checkInstalled(String clusterId, String componentName);

}
