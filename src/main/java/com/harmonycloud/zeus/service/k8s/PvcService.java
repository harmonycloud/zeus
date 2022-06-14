package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.PersistentVolumeClaim;

import java.util.List;
import java.util.Map;

/**
 * @author dengyulong
 * @date 2021/04/01
 */
public interface PvcService {

    /**
     * 查询存储列表
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @return List<PersistentVolumeClaim>
     */
    List<PersistentVolumeClaim> list(String clusterId, String namespace);

    /**
     * 查询存储列表
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param fields    关键词过滤
     * @return List<PersistentVolumeClaim>
     */
    List<PersistentVolumeClaim> listWithFields(String clusterId, String namespace, Map<String, String> fields);

}
