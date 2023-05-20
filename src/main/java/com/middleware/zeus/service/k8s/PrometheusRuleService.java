package com.middleware.zeus.service.k8s;

import com.middleware.zeus.common.model.middleware.MiddlewareAlertsDTO;
import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRule;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/4/27 10:45 上午
 */
public interface PrometheusRuleService {

    /**
     * 获取告警configmap
     *
     * @param clusterId 集群id
     * @param namespace
     * @param name
     * @return PrometheusRule
     */
    PrometheusRule get(String clusterId, String namespace, String name);

    /**
     * 获取告警configmap
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param labels 标签
     * @return PrometheusRule
     */
    List<PrometheusRule> list(String clusterId, String namespace, Map<String, String> labels);

    /**
     * 更新告警configmap
     *
     * @param clusterId 集群id
     * @param prometheusRule cr
     */
    void update(String clusterId, PrometheusRule prometheusRule);

    /***
     * 封装prometheus对象
     *
     * @param prometheusRule 告警规则文件
     * @return
     */
    List<MiddlewareAlertsDTO> convertPrometheusRule(PrometheusRule prometheusRule);

}
