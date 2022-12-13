package com.harmonycloud.zeus.service.k8s;

import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.bean.BeanK8sDefaultCluster;

/**
 * @author xutianhong
 * @Date 2021/8/3 3:56 下午
 */
public interface K8sDefaultClusterService {

    /**
     * 获取默认集群
     *
     * @return BeanK8sDefaultCluster
     */
    BeanK8sDefaultCluster get();

    /**
     * 记录默认集群
     * @param cluster
     *
     * @return BeanK8sDefaultCluster
     */
    void create(MiddlewareClusterDTO cluster);

    /**
     * 删除默认集群
     * @param cluster
     *
     * @return BeanK8sDefaultCluster
     */
    void delete(String clusterId);

}
