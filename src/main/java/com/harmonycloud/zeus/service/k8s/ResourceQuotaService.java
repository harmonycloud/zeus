package com.harmonycloud.zeus.service.k8s;

import java.util.List;
import java.util.Map;

import com.middleware.caas.common.model.middleware.ResourceQuotaDTO;

/**
 * @author dengyulong
 * @date 2021/04/01
 */
public interface ResourceQuotaService {

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
    Map<String, List<String>> statistics(String clusterId);

    /**
     * 查询resource quota
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @return
     */
    Map<String, List<String>> list(String clusterId, String namespace);

    /**
     * 查询resource quota
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name      名称
     * @return
     */
    Map<String, List<String>> get(String clusterId, String namespace, String name);
}
