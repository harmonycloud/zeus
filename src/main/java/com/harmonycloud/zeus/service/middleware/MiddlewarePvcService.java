package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.EventDetail;
import com.harmonycloud.caas.common.model.middleware.Backup;
import com.harmonycloud.caas.common.model.middleware.MiddlewarePvcDto;
import com.harmonycloud.zeus.integration.cluster.bean.Maintenance;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/1/10 10:13 上午
 */
public interface MiddlewarePvcService {

    /**
     * 查询中间件存储信息
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param type 中间件类型
     */
    List<MiddlewarePvcDto> list(String clusterId, String namespace, String middlewareName, String type);

    /**
     * 查询中间件存储信息
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param pvcName pvc名称
     */
    List<EventDetail> getEvent(String clusterId, String namespace, String middlewareName, String pvcName);

    /**
     * 查询备份列表
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param pvcName pvc名称
     * @param type 中间件类型
     * @param storage 当前存储大小
     * @param targetStorage 目标存储大小
     */
    void scalePvc(String clusterId, String namespace, String middlewareName, String pvcName, String type, String storageClass, Double storage, Double targetStorage);

    /**
     * 查询备份列表
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param pvcName pvc名称
     */
    void rollback(String clusterId, String namespace, String middlewareName, String pvcName);

    /**
     * 查询备份列表
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param pvcName pvc名称
     *
     * @return String
     */
    Map<String, String> getPvcStatus(String clusterId, String namespace, String middlewareName, String pvcName);

}
