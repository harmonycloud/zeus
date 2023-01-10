package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/1/10 10:05 上午
 */
@Accessors(chain = true)
@Data
public class MaintenanceSpecRule {

    private Map<String, String> selector;

}
