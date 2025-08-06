package com.middleware.zeus.integration.cluster.bean.mongodb;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.*;
import lombok.experimental.Accessors;

import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;

/**
 * @author xutianhong
 * @Date 2025/8/1 11:07
 */
@Accessors(chain = true)
@Group("mongodb.com")
@Version(V1)
@Plural("opsmanagers")
@Kind("MongoDBOpsManager")
@Singular("opsmanager")
public class OpsManager extends CustomResource<OpsManagerSpec, OpsManagerStatus> implements Namespaced {
}
