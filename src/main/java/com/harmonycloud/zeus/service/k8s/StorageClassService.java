package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.middleware.StorageClass;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
public interface StorageClassService {

    /**
     * 查询存储服务列表
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param onlyMiddleware 是否只返回支持中间件的存储
     * @return
     */
    List<StorageClass> list(String clusterId, String namespace, boolean onlyMiddleware);

}
