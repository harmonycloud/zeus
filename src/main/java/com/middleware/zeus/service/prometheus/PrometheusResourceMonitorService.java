package com.middleware.zeus.service.prometheus;

import com.middleware.zeus.common.model.PrometheusResponse;

import java.util.Map;

/**
 * @author xutianhong
 * @Date 2022/2/25 10:49 上午
 */
public interface PrometheusResourceMonitorService {

    /**
     * 查询资源监控数据(单返回数据)
     *
     * @param query 查询语句
     * @param clusterId 集群id
     * @return Double
     */
    Double queryAndConvert(String clusterId, String query);

    /**
     * 查询资源监控数据(多返回数据)
     *
     * @param query 查询语句
     * @param clusterId 集群id
     * @return Double
     */
    Map<String, Double> queryPvcs(String clusterId, String query);

    /**
     * 查询资源监控数据
     *
     * @param query 查询语句
     * @param clusterId 集群id
     * @return PrometheusResponse
     */
    PrometheusResponse query(String clusterId, String query) throws Exception;

    /**
     * 查询资源监控数据
     *
     * @param query 查询语句
     * @param clusterId 集群id
     * @return PrometheusResponse
     */
    Map<String, Double> sumResponseByTarget(PrometheusResponse response, String target) throws Exception;

}
