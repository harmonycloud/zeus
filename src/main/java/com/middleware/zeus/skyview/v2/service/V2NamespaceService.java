package com.middleware.zeus.skyview.v2.service;

import com.middleware.zeus.common.model.middleware.Namespace;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/26 6:35 下午
 */
public interface V2NamespaceService {

    /**
     * 查询分区列表
     * @param clusterId 集群id
     * @param detail 详情
     *
     * @return List<Namespace>
     */
    List<Namespace> list(String clusterId, Boolean detail);

}
