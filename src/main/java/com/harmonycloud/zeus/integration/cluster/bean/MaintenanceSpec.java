package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/1/10 9:55 上午
 */
@Accessors(chain = true)
@Data
public class MaintenanceSpec {

    private String action;

    private List<MaintenancePvc> pvcs;

    private MaintenanceSpecRule nodeRule;

    private MaintenanceSpecRule podRule;

    private Map<String, String> param;

    private MaintenanceStatus status;



}
