package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/4/26 11:03 上午
 */
@Data
@Accessors(chain = true)
@ApiModel(description = "prometheus告警")
public class PrometheusAlerts {

    private String state;

    private String activeAt;

    private String value;

    private Map<String, String> labels;

    private Map<String, String> annotations;

}
