package com.middleware.zeus.service.components;

import com.middleware.zeus.common.model.ClusterComponentsDto;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.bean.BeanClusterComponents;

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
     * @param clusterComponentsDto 集群组件对象
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

    /**
     * 其他需要执行的操作
     * @param clusterComponentsDto
     */
    void expand(ClusterComponentsDto clusterComponentsDto);

    /**
     * 读取全局参数
     * @param clusterComponentsDto
     */
    void readSystemConfig(ClusterComponentsDto clusterComponentsDto);

}
