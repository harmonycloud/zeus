package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.zeus.integration.cluster.bean.Maintenance;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/1/10 4:08 下午
 */
public interface MaintenanceService {

    /**
     * 查询Maintenance
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param labels 标签
     */
    List<Maintenance> list(String clusterId, String namespace, Map<String, String> labels);

    /**
     * 查询Maintenance
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName  中间件名称
     * @param action 运维动作
     */
    List<Maintenance> list(String clusterId, String namespace, String middlewareName, String action);

    /**
     * 存储扩容
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param pvcNameList  pvc名称列表
     * @param targetStorage 目标存储大小
     * @param labels 标签
     */
    void scaleStorage(String clusterId, String namespace, String middlewareName, List<String> pvcNameList, Double targetStorage, Map<String, String> labels);

    /**
     * 存储扩容
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param middlewareName 中间件名称
     * @param pvcNameList  pvc名称列表
     * @param targetStorage 目标存储大小
     * @param labels 标签
     */
    void rollBack(String clusterId, String namespace, String middlewareName, List<String> pvcNameList, Double targetStorage, Map<String, String> labels);

    /**
     * 删除
     */
    void delete();

}
