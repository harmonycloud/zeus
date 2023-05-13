package com.middleware.zeus.service.k8s;

import java.util.List;

import com.middleware.zeus.common.model.ResourceQuotaDo;
import com.middleware.zeus.common.model.middleware.ResourceQuotaDTO;

/**
 * @author dengyulong
 * @date 2021/04/01
 */
public interface ResourceQuotaService {

    /**
     * add resource quota
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param resourceQuotaDo 资源配额对象
     */
    void create(String clusterId, String namespace, ResourceQuotaDo resourceQuotaDo);

    /**
     * add resource quota
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param resourceQuotaDo 资源配额对象
     */
    void update(String clusterId, String namespace, ResourceQuotaDo resourceQuotaDo);

    /**
     * 查询resource quota
     *
     * @param clusterId 集群id
     * @return
     */
    List<ResourceQuotaDTO> list(String clusterId);

    /**
     * 查询resource quota
     *
     * @param clusterId 集群id
     * @return
     */
    ResourceQuotaDo statistics(String clusterId);

    /**
     * 查询resource quota
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @return
     */
    ResourceQuotaDo list(String clusterId, String namespace);

    /**
     * 查询resource quota
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param storageClass 存储类型
     * @return
     */
    ResourceQuotaDo list(String clusterId, String namespace, String storageClass);

    /**
     * 查询resource quota
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name      名称
     * @return
     */
    ResourceQuotaDo get(String clusterId, String namespace, String name);

    /**
     * 统计资源配额
     *
     * @param resourceQuotaDoList 资源配额集合
     * @return
     */
    ResourceQuotaDo calculateQuota(List<ResourceQuotaDo> resourceQuotaDoList);
}

