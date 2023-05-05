package com.middleware.zeus.integration.cluster.bean.prometheus;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/27 10:31 上午
 */
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrometheusRules {

    private String alert;

    private String expr;

    @JsonProperty(value = "for")
    private String time;

    private Map<String, String> annotations;

    private Map<String, String> labels;

    private String record;
}
