package com.harmonycloud.zeus.service.k8s;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/1/10 4:08 下午
 */
public interface MaintenanceService {

    /**
     * 存储扩容
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param pvcNameList  pvc名称列表
     * @param targetStorage 目标存储大小
     */
    void scaleStorage(String clusterId, String namespace, List<String> pvcNameList, Double targetStorage);

}
