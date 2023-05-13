package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/3/31 4:32 下午
 */
@Data
@ApiModel(description = "普罗米修斯返回数据")
public class PrometheusResult {

    private Map<String,String> metric;

    private List<String> value;

    private List<List<String>>  values;

}
