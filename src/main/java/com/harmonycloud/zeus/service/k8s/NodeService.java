package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.Node;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
public interface NodeService {

    /**
     * 查询节点列表
     *
     * @param clusterId 集群id
     * @return
     */
    List<Node> list(String clusterId);

    /**
     * 设置k8s版本
     *
     * @param cluster 集群信息
     */
    void setClusterVersion(MiddlewareClusterDTO cluster);

    /**
     * 查询污点列表
     * @param clusterId
     * @return
     */
    List<String> listTaints(String clusterId);

    /**
     * 获取一个nodeip
     * @param clusterId
     * @return
     */
    String getNodeIp(String clusterId);

}
