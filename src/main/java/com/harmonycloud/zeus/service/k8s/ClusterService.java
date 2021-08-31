package com.harmonycloud.zeus.service.k8s;

import java.util.List;

import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
public interface ClusterService {

    /**
     * 获取所有集群
     * @return
     */
    List<MiddlewareClusterDTO> listClusters();

    /**
     * 获取所有集群
     *
     * @param detail 是否返回明细信息
     * @return
     */
    List<MiddlewareClusterDTO> listClusters(boolean detail);

    /**
     * 设置集群属性
     *
     * @param clusters 集群列表
     */
    void initClusterAttributes(List<MiddlewareClusterDTO> clusters);

    /**
     * 设置集群属性
     *
     * @param cluster 集群
     */
    void initClusterAttributes(MiddlewareClusterDTO cluster);

    /**
     * 根据集群id查询集群
     *
     * @param clusterId 集群id
     * @return
     */
    MiddlewareClusterDTO findById(String clusterId);

    /**
     * 根据集群id查询并校验制品服务
     *
     * @param clusterId 集群id
     * @return
     */
    MiddlewareClusterDTO findByIdAndCheckRegistry(String clusterId);

    /**
     * 添加集群
     *
     * @param cluster 集群信息
     */
    void addCluster(MiddlewareClusterDTO cluster);

    /**
     * 修改集群
     *
     * @param cluster 集群信息
     */
    void updateCluster(MiddlewareClusterDTO cluster);

    /**
     * 修改集群（直接更新）
     *
     * @param cluster 集群信息
     */
    void update(MiddlewareClusterDTO cluster);

    /**
     * 移除集群
     *
     * @param clusterId 集群id
     */
    void removeCluster(String clusterId);

}
