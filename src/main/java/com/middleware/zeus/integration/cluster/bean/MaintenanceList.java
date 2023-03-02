package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/1/10 8:42 下午
 */
@Accessors(chain = true)
@Data
public class MaintenanceList {

    private String apiVersion = "maintenance.middleware.hc.cn/v1alpha1";

    private String kind = "Maintenance";

    private ObjectMeta metadata;

    private List<Maintenance> items;

}
