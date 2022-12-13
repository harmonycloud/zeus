package com.harmonycloud.zeus.service.components.api;

import com.middleware.caas.common.model.ClusterComponentsDto;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.service.components.BaseComponentsService;

/**
 * @author xutianhong
 * @Date 2021/10/29 4:16 下午
 */
public interface LoggingService extends BaseComponentsService {

    /**
     * 安装log-pilot组件
     *
     * @param cluster  集群对象
     * @param clusterComponentsDto  集群组件对象
     */
    void logPilot(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto);
}
