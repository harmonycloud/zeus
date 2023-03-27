package com.middleware.zeus.skyview.v2.service;

import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/26 4:32 下午
 */
public interface V2ClusterService {


    /**
     * 查询集群列表
     *
     */
    List<MiddlewareClusterDTO> list();

    /**
     * 查询集群详情
     *
     */
    MiddlewareClusterDTO get(String clusterId);

}
