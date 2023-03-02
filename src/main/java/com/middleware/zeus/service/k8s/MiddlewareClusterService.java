package com.middleware.zeus.service.k8s;

import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.bean.BeanMiddlewareCluster;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCluster;

import java.util.List;

/**
 * @author yushuaikang
 * @date 2022/3/25 上午11:23
 */
public interface MiddlewareClusterService {

    /**
     * 创建集群
     */
    void create(String clusterId, MiddlewareCluster middlewareCluster);

    /**
     * 查询集群列表
     * @return
     */
    List<MiddlewareClusterDTO> listClusterDtos();

    /**
     * 查询集群列表
     */
    List<MiddlewareCluster> listClusters();

    /**
     * 查询集群列表
     * @param clusterId
     * @return
     */
    List<MiddlewareCluster> listClusters(String clusterId);

    /**
     * 修改集群
     */
    void update(String clusterId, MiddlewareCluster middlewareCluster);

    /**
     * 删除集群
     */
    void delete(String clusterId);

    /**
     * 查询集群列表
     */
    List<BeanMiddlewareCluster> listClustersByClusterId(String clusterId);
}
