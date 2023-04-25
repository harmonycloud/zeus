package com.middleware.zeus.integration.cluster;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRule;
import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRuleList;
import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

/**
 * @author xutianhong
 * @Date 2021/4/27 10:04 上午
 */
@Component
public class PrometheusRuleWrapper {

    /**
     * 获取告警规则
     */
    public PrometheusRule get(String clusterId, String namespace, String name) {
        // init client
        NonNamespaceOperation<PrometheusRule, PrometheusRuleList, Resource<PrometheusRule>> prometheusRuleClient =
            K8sClient.getClient(clusterId).resources(PrometheusRule.class, PrometheusRuleList.class)
                .inNamespace(namespace);
        // get
        return prometheusRuleClient.withName(name).get();
    }

    /**
     * 更新告警规则
     */
    public void update(String clusterId, PrometheusRule prometheusRule) throws IOException {
        // init client
        NonNamespaceOperation<PrometheusRule, PrometheusRuleList, Resource<PrometheusRule>> prometheusRuleClient =
            K8sClient.getClient(clusterId).resources(PrometheusRule.class, PrometheusRuleList.class);
        // update
        prometheusRuleClient.resource(prometheusRule).update();
    }

}
