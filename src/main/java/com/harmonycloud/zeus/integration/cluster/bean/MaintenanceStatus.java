package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/1/10 9:59 上午
 */
@Accessors(chain = true)
@Data
public class MaintenanceStatus {

    private String phase;

    private List<Map<String, String>> conditions;

}
