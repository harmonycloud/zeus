package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * @author xutianhong
 * @Date 2021/3/31 4:31 下午
 */
@Data
@ApiModel(description = "普罗米修斯返回数据")
public class PrometheusResponse {

    private String status;

    private PrometheusData data;

    private String error;

    private String errorType;
}
