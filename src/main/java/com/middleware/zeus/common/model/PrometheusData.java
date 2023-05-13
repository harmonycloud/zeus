package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/3/31 4:32 下午
 */
@Data
@ApiModel(description = "普罗米修斯返回数据")
public class PrometheusData {

    private String resultType;

    private List<PrometheusResult> result;
}
