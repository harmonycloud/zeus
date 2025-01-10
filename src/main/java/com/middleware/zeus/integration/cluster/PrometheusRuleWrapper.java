package com.middleware.zeus.integration.cluster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRule;
import com.middleware.zeus.integration.cluster.bean.prometheus.PrometheusRuleList;
import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.springframework.util.CollectionUtils;

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
     * 根据标签获取告警规则列表
     */
    public List<PrometheusRule> list(String clusterId, String namespace, Map<String, String> labels) {
        if (CollectionUtils.isEmpty(labels)){
            labels = new HashMap<>();
        }
        // init client
        NonNamespaceOperation<PrometheusRule, PrometheusRuleList, Resource<PrometheusRule>> prometheusRuleClient =
                K8sClient.getClient(clusterId).resources(PrometheusRule.class, PrometheusRuleList.class);
        // 条件判断
        if (StringUtils.isNotEmpty(namespace)) {
            prometheusRuleClient =
                    ((MixedOperation<PrometheusRule, PrometheusRuleList, Resource<PrometheusRule>>)prometheusRuleClient)
                            .inNamespace(namespace);
        }
        // list
        PrometheusRuleList prometheusRuleList = prometheusRuleClient.withLabels(labels).list();
        if (prometheusRuleList == null || CollectionUtils.isEmpty(prometheusRuleList.getItems())) {
            return new ArrayList<>(0);
        }
        return prometheusRuleList.getItems();
    }

    /**
     * 更新告警规则
     */
    public void update(String clusterId, PrometheusRule prometheusRule) throws IOException {
        // init client
        NonNamespaceOperation<PrometheusRule, PrometheusRuleList, Resource<PrometheusRule>> prometheusRuleClient =
                K8sClient.getClient(clusterId).resources(PrometheusRule.class, PrometheusRuleList.class);
        // update
        prometheusRuleClient.resource(prometheusRule).patch();
    }

    /**
     * 创建告警规则
     */
    public void create(String clusterId, PrometheusRule prometheusRule) throws IOException {
        // init client
        NonNamespaceOperation<PrometheusRule, PrometheusRuleList, Resource<PrometheusRule>> prometheusRuleClient =
                K8sClient.getClient(clusterId).resources(PrometheusRule.class, PrometheusRuleList.class);
        // create
        prometheusRuleClient.resource(prometheusRule).create();
    }

    /**
     * 删除告警规则
     */
    public void delete(String clusterId, String namespace, String name) throws IOException {
        // init client
        NonNamespaceOperation<PrometheusRule, PrometheusRuleList, Resource<PrometheusRule>> prometheusRuleClient =
                K8sClient.getClient(clusterId).resources(PrometheusRule.class, PrometheusRuleList.class).inNamespace(namespace);
        // delete
        prometheusRuleClient.withName(name).delete();
    }

}
