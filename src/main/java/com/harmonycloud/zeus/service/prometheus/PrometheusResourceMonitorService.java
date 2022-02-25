package com.harmonycloud.zeus.service.prometheus;

import com.harmonycloud.caas.common.model.PrometheusResponse;

/**
 * @author xutianhong
 * @Date 2022/2/25 10:49 上午
 */
public interface PrometheusResourceMonitorService {

    /**
     * 查询资源监控数据
     *
     * @param query 查询语句
     * @param clusterId 集群id
     * @return Double
     */
    Double queryAndConvert(String clusterId, String query);

    /**
     * 查询资源监控数据
     *
     * @param query 查询语句
     * @param clusterId 集群id
     * @return PrometheusResponse
     */
    PrometheusResponse query(String clusterId, String query) throws Exception;

}
