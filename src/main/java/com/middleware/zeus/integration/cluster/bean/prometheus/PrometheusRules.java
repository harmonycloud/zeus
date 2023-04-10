package com.middleware.zeus.integration.cluster.bean.prometheus;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/4/27 10:31 上午
 */
@Data
@Accessors(chain = true)
public class PrometheusRules {

    private String alert;

    private String expr;

    @JSONField(name = "for")
    private String time;

    private Map<String, String> annotations;

    private Map<String, String> labels;

    private String record;
}
