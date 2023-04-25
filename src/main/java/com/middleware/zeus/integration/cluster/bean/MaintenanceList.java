package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/1/10 8:42 下午
 */
@Accessors(chain = true)
public class MaintenanceList extends DefaultKubernetesResourceList<Maintenance> {

}
