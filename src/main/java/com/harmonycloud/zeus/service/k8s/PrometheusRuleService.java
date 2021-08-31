package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.zeus.integration.cluster.bean.prometheus.PrometheusRule;

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
     * 更新告警configmap
     *
     * @param clusterId 集群id
     * @param prometheusRule cr
     */
    void update(String clusterId, PrometheusRule prometheusRule);

}
