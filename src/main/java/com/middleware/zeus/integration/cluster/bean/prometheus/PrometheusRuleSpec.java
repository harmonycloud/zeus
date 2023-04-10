package com.middleware.zeus.integration.cluster.bean.prometheus;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/4/27 10:26 上午
 */
@Data
@Accessors(chain = true)
public class PrometheusRuleSpec {

    private List<PrometheusRuleGroups> groups;

}
