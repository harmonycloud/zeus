package com.middleware.zeus.integration.cluster.bean.prometheus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/27 10:24 上午
 */
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value=JsonInclude.Include.NON_NULL)
public class PrometheusRule {

    private String apiVersion = "monitoring.coreos.com/v1";

    private String kind;

    private ObjectMeta metadata;

    private PrometheusRuleSpec spec;

}
