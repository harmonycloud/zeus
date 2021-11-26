package com.harmonycloud.zeus.integration.cluster;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.*;

import java.io.IOException;
import java.util.Map;

import com.harmonycloud.zeus.integration.cluster.bean.prometheus.PrometheusRule;
import com.harmonycloud.zeus.util.K8sClient;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

/**
 * @author xutianhong
 * @Date 2021/4/27 10:04 上午
 */
@Component
public class PrometheusRuleWrapper {

    private static final CustomResourceDefinitionContext CONTEXT =
        new CustomResourceDefinitionContext.Builder().withGroup(MONITORING_CORS_COM)
            .withVersion(MIDDLEWARE_CLUSTER_VERSION).withScope(NAMESPACED).withPlural(PROMETHEUS_RULE).build();

    /**
     * 获取告警规则
     */
    public PrometheusRule get(String clusterId, String namespace, String name) {
//        K8sClient.getClient(clusterId).customResource(CONTEXT).li
        Map<String, Object> map = K8sClient.getClient(clusterId).customResource(CONTEXT).get(namespace, name);
        return JSONObject.parseObject(JSONObject.toJSONString(map), PrometheusRule.class);
    }

    /**
     * 更新告警规则
     */
    public void update(String clusterId, PrometheusRule prometheusRule) throws IOException {
        K8sClient.getClient(clusterId).customResource(CONTEXT).createOrReplace(
            prometheusRule.getMetadata().getNamespace(),
            JSONObject.parseObject(JSONObject.toJSONString(prometheusRule)));
    }

}
