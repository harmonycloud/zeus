package com.middleware.zeus.service.k8s;

import com.middleware.zeus.common.model.k8s.PvDo;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/1/10 11:25 上午
 */
public interface PvService {

    /**
     * 查询中间件pv
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param pvcNameList pvc名称
     * @return List<PvDo>
     */
    List<PvDo> listPv(String clusterId, String namespace, List<String> pvcNameList);

}
