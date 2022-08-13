package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.ActiveAreaDto;
import com.harmonycloud.caas.common.model.ClusterNodeResourceDto;
import com.harmonycloud.caas.common.model.Node;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;

import java.util.List;
import java.util.Map;

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
     * 查询节点列表
     *
     * @param clusterId 集群id
     * @param labels 标签
     * @return
     */
    List<Node> list(String clusterId, Map<String, String> labels);

    /**
     * 获取可用区资源情况
     *
     * @param clusterId 集群id
     * @return
     */
    List<Node> listAllocatableNode(String clusterId);

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
     * 封装node信息
     * @param nodes 节点k8s对象
     * @return nodeList
     */
    List<Node> convertToDto(List<io.fabric8.kubernetes.api.model.Node> nodes);

    /**
     * 查询节点资源
     * @param clusterId 集群id
     * @param nodes 节点列表
     * @param all 是否全部
     * @return clusterNodeResourceDtoList
     */
    List<ClusterNodeResourceDto> getNodeResource(String clusterId, List<Node> nodes, Boolean all);

    /**
     * 查询节点资源并求和
     * @param clusterId 集群id
     * @param nodes 节点列表
     * @param all 是否全部
     * @return clusterNodeResourceDto
     */
    ClusterNodeResourceDto getSumNodeResource(String clusterId, List<Node> nodes, Boolean all);
}
