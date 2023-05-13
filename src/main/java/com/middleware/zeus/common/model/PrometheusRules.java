package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/4/26 10:54 上午
 */
@Data
@Accessors(chain = true)
@ApiModel(description = "prometheus规则")
public class PrometheusRules {

    private String name;

    private String query;

    private Double duration;

    private Map<String, String> labels;

    private Map<String, String> annotations;

    private List<PrometheusAlerts> alerts;

    private String health;

    private String type;

}
