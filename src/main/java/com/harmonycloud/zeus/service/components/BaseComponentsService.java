package com.harmonycloud.zeus.service.components;

import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;

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
    void deploy(MiddlewareClusterDTO cluster, String type);

    /**
     * 接入组件
     *
     * @param cluster 集群信息
     */
    void integrate(MiddlewareClusterDTO cluster);

    /**
     * 卸载组件
     *
     * @param cluster 集群信息
     */
    void uninstall(MiddlewareClusterDTO cluster, String type);

}
