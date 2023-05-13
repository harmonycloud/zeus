package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/4/26 10:49 上午
 */
@Data
@Accessors(chain = true)
@ApiModel(description = "prometheus规则组")
public class PrometheusGroups {

    private String name;

    private String file;

    private List<PrometheusRules> rules;

    private Integer interval;

}
