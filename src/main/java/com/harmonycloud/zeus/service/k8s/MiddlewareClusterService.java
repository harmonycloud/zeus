package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.zeus.bean.BeanMiddlewareCluster;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCluster;

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
     */
    List<MiddlewareCluster> listClusters();

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
