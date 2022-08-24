package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.ClusterDTO;
import com.harmonycloud.caas.common.model.ClusterNamespaceResourceDto;
import com.harmonycloud.caas.common.model.ClusterNodeResourceDto;
import com.harmonycloud.caas.common.model.Node;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
public interface ClusterService {

    /**
     * 获取所有集群
     *
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
     * 获取所有集群
     *
     * @param detail 是否返回明细信息
     * @return
     */
    List<MiddlewareClusterDTO> listClusters(boolean detail, String key, String projectId);

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
     * 根据集群id查询集群
     *
     * @param clusterId 集群id
     * @return
     */
    MiddlewareClusterDTO detail(String clusterId);

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
     * 过滤不需要展示的mw，未注册分区下的中间件不做统计
     * @param clusterId
     * @param mwCrdList
     * @return
     */
    List<MiddlewareCR> filterByNamespace(String clusterId, List<MiddlewareCR> mwCrdList);

    /**
     * 获取集群主机资源列表
     *
     * @param clusterId 集群id
     * @return List<Node>
     */
    List<ClusterNodeResourceDto> getNodeResource(String clusterId) throws Exception;

    /**
     * 获取集群主机资源列表
     *
     * @param clusterId 集群id
     * @return List<Node>
     */
    List<ClusterNamespaceResourceDto> getNamespaceResource(String clusterId) throws Exception;

    /**
     * 获取快捷添加集群curl指令
     *
     * @param clusterName 集群名称
     * @param apiAddress  接口前缀
     * @param userToken
     * @return
     */
    String getClusterJoinCommand(String clusterName, String apiAddress, String userToken, Registry registry);

    /**
     * curl指令快捷添加集群
     *
     * @param adminConf
     * @param name      集群名称
     * @return
     */
    BaseResult quickAdd(MultipartFile adminConf, String name, Registry registry);

    /**
     * 获取集群已注册分区
     *
     * @param clusterId 集群id
     * @return
     */
    List<Namespace> listRegisteredNamespace(String clusterId);

    /**
     * 将观云台clusterid转换为中间件平台clusterid
     * @param skyviewClusterId
     * @return
     */
    String convertToSkyviewClusterId(String skyviewClusterId);

    /**
     * 将中间件平台clusterid转为观云台clusterid
     * @param zeusClusterId
     * @return
     */
    String convertToZeusClusterId(String zeusClusterId);

    /**
     * 根据观云台集群id查找观云台集群信息
     * @param skyviewClusterId
     * @return
     */
    ClusterDTO findBySkyviewClusterId(String skyviewClusterId);

    /**
     * 根据集群id查询集群监控资源
     *
     * @param clusterId 集群id
     * @return
     */
    ClusterQuotaDTO monitoring(String clusterId);

}
