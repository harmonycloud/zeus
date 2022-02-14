package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareInfoDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareOperatorDTO;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;

import java.io.File;
import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public interface MiddlewareInfoService {

    /**
     * 查询中间件列表
     *
     * @return List<BeanMiddlewareInfo>
     */
    List<BeanMiddlewareInfo> list(Boolean all);

    /**
     * 查询集群已安装所有中间件
     * @param clusterDTOList 集群集合
     * @return
     */
    List<BeanMiddlewareInfo> listInstalledByClusters(List<MiddlewareClusterDTO> clusterDTOList);

    /**
     * 查询集群已安装所有中间件
     * @param clusterDTO 集群
     * @return
     */
    List<BeanMiddlewareInfo> listInstalledByCluster(MiddlewareClusterDTO clusterDTO);
    /**
     * 查询中间件列表
     *
     * @param type 类型
     * @return List<BeanMiddlewareInfo>
     */
    List<BeanMiddlewareInfo> listByType(String type);

    /**
     * 查询指定中间件
     *
     * @param chartName chart名称
     * @param chartVersion chart版本
     * @return BeanMiddlewareInfo
     */
    BeanMiddlewareInfo get(String chartName, String chartVersion);

    /**
     * 查询中间件列表
     *
     * @param clusterId 集群id
     * @return
     */
    List<MiddlewareInfoDTO> list(String clusterId);

    /**
     * 查询中间件版本信息
     *
     * @param clusterId 集群id
     * @param type 类型
     * @return
     */
    List<MiddlewareInfoDTO> version(String clusterId, String type);

    /**
     * 更新中间件信息
     *
     * @param middlewareInfo 中间件内容
     */
    void update(BeanMiddlewareInfo middlewareInfo);

    /**
     * 添加中间件信息
     *
     * @param helmChartFile 中间件内容
     * @param file 文件
     */
    void insert(HelmChartFile helmChartFile, File file);

    /**
     * 下架中间件指定版本
     *
     * @param chartName 名称
     * @param chartVersion 版本
     */
    void delete(String chartName, String chartVersion);

    /**
     * 获取集群控制器状态信息
     * @param clusterList
     * @return
     */
    MiddlewareOperatorDTO getOperatorInfo(List<MiddlewareClusterDTO> clusterList);
}
