package com.harmonycloud.zeus.service.components;

import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.bean.BeanClusterComponents;

/**
 * @author xutianhong
 * @Date 2021/10/29 2:28 下午
 */
public interface BaseComponentsService {

    /**
     * 确定对应service
     *
     * @param name 组件名称
     * @return boolean
     */
    boolean support(String name);

    /**
     * 部署组件
     *
     * @param cluster 集群信息
     * @param type 部署模式:高可用，单实例
     */
    void deploy(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto);

    /**
     * 卸载/取消接入 组件
     *
     * @param cluster 集群信息
     * @param status  状态
     */
    void delete(MiddlewareClusterDTO cluster, Integer status);

    /**
     * 更新组件状态
     *
     * @param cluster 集群信息
     * @param beanClusterComponents 组件对象
     */
    void updateStatus(MiddlewareClusterDTO cluster, BeanClusterComponents beanClusterComponents);

    /**
     * 处理特殊状态
     *
     * @param clusterComponentsDto 集群组件对象
     * @return List<ClusterComponentsDto>
     */
    void setStatus(ClusterComponentsDto clusterComponentsDto);

}
