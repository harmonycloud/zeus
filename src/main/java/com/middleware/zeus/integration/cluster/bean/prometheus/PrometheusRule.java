package com.middleware.zeus.integration.cluster.bean.prometheus;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.experimental.Accessors;

import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;

/**
 * @author xutianhong
 * @Date 2021/4/27 10:24 上午
 */
@Accessors(chain = true)
@Group(MONITORING_CORS_COM)
@Version(MIDDLEWARE_CLUSTER_VERSION)
@Plural(PROMETHEUS_RULE)
public class PrometheusRule extends CustomResource<PrometheusRuleSpec, Void> implements Namespaced {

}
