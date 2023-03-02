package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2023/1/10 9:54 上午
 */
@Accessors(chain = true)
@Data
public class Maintenance {

    private String apiVersion = "maintenance.middleware.hc.cn/v1alpha1";

    private String kind = "Maintenance";

    private ObjectMeta metadata;

    private MaintenanceSpec spec;

    private MaintenanceStatus status;

}
