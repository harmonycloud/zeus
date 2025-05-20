package com.middleware.zeus.service.middleware;

import com.middleware.zeus.common.model.MiddlewareVersionDto;
import com.middleware.zeus.common.model.middleware.*;
import com.middleware.zeus.common.model.registry.HelmChartFile;
import com.middleware.zeus.bean.BeanMiddlewareInfo;

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
     * 查询中间件列表
     *
     * @return List<BeanMiddlewareInfo>
     */
    List<BeanMiddlewareInfo> filter(List<BeanMiddlewareInfo> beanMiddlewareInfoList);

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
     * 查询当前集群绑定的中间件信息
     *
     * @param clusterId 集群id
     * @param type      类型
     * @return
     */
    MiddlewareInfoDTO getByType(String clusterId, String type);

    /**
     * 查询中间件版本信息
     *
     * @param clusterId 集群id
     * @param type 类型
     * @return
     */
    List<MiddlewareInfoDTO> chartVersion(String clusterId, String type);

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

    /**
     * 已纳管集群下服务
     * @param type
     * @param keyword
     * @return
     */
    List<Middleware> middlewareList(String type, String keyword);

    /**
     * 查询多集群中间件列表
     *
     * @return
     */
    List<MiddlewareInfoDTO> clusterList();

    /**
     * 查询指定中间件发布时可指定版本
     *
     * @param type 类型
     * @param chartVersion 中间件chart版本
     * @return List<MiddlewareVersionDto>
     */
    List<MiddlewareVersionDto> version(String type, String chartVersion, String clusterId, Boolean filter);

    /**
     * 查询指定中间件的禁用版本列表
     *
     * @param clusterId 集群id
     * @param type 类型
     * @param chartVersion 中间件chart版本
     * @return List<MiddlewareDisableVersionDto>
     */
    List<MiddlewareDisableVersionDto> disableVersion(String clusterId, String type, String chartVersion);

    /**
     * 设置指定中间件的禁用版本列表
     *
     * @param middlewareDisableVersionDtoList 禁用版本列表
     */
    void setDisableVersion(List<MiddlewareDisableVersionDto> middlewareDisableVersionDtoList);


    /**
     * 查询用户在指定项目下可见的operator
     * @param clusterId
     * @return
     */
    List<MiddlewareInfoDTO> listUsersOperator(String clusterId);


}
