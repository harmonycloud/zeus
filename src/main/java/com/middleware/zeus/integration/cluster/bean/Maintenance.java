package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.experimental.Accessors;

import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;

/**
 * @author xutianhong
 * @Date 2023/1/10 9:54 上午
 */
@Accessors(chain = true)
@Group(MAINTENANCE_MIDDLEWARE_HC_CN)
@Version(V1_ALPHA1)
@Plural(MAINTENANCES)
public class Maintenance extends CustomResource<MaintenanceSpec, MaintenanceStatus> implements Namespaced {

}
