package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.ClusterNodeResourceDto;
import com.harmonycloud.caas.common.model.Node;
import com.harmonycloud.caas.common.model.middleware.*;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
public interface ClusterService {

    /**
     * 获取指定集群
     * @return
     */
    MiddlewareClusterDTO get(String clusterId);

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
    List<MiddlewareClusterDTO> listClusters(boolean detail, String key);

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


    /**
     * 获取集群注册的分区
     *
     * @param clusterDTOList 集群dto集合
     * @return
     */
    List<Namespace> getRegisteredNamespaceNum(List<MiddlewareClusterDTO> clusterDTOList);

    /**
     * 获取集群资源配额及使用量
     *
     * @param clusterDTOList 集群dto集合
     * @return
     */
    ClusterQuotaDTO getClusterQuota(List<MiddlewareClusterDTO> clusterDTOList);

    /**
     * 获取集群下服务资源列表
     *
     * @param clusterId 集群id
     * @return List<MiddlewareResourceInfo>
     */
    List<MiddlewareResourceInfo> getMwResource(String clusterId) throws Exception;

    /**
     * 获取集群主机资源列表
     *
     * @param clusterId 集群id
     * @return List<Node>
     */
    List<ClusterNodeResourceDto> getNodeResource(String clusterId) throws Exception;


}
