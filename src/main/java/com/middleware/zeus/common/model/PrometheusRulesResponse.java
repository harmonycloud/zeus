package com.middleware.zeus.common.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/26 11:45 上午
 */
@Data
@Accessors(chain = true)
public class PrometheusRulesResponse {

    private String status;

    private PrometheusRulesData data;

}
