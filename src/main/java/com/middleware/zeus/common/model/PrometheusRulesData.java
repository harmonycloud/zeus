package com.middleware.zeus.common.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/4/26 1:45 下午
 */
@Data
@Accessors(chain = true)
public class PrometheusRulesData {

    private List<PrometheusGroups> groups;

}
