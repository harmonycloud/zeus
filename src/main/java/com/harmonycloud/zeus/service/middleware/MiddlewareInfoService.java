package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.middleware.MiddlewareInfoDTO;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public interface MiddlewareInfoService {

    /**
     * 查询中间件列表
     *
     * @return
     */
    List<BeanMiddlewareInfo> list(String clusterId);


    BeanMiddlewareInfo getMiddlewareInfo(String chartName, String chartVersion);

    /**
     * 查询中间件列表
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @return
     */
    List<MiddlewareInfoDTO> list(String clusterId, String namespace);

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
     */
    void insert(HelmChartFile helmChartFile, String path, String clusterId);

}
