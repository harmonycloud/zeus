package com.middleware.zeus.integration.cluster.bean.mongodb;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.experimental.Accessors;

import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.V1;

/**
 * @author xutianhong
 * @Date 2025/8/1 12:53
 */
@Accessors(chain = true)
@Group("mongodb.com")
@Version(V1)
@Plural("mongodb")
public class Mongodb extends CustomResource<MongodbSpec, MongodbStatus> implements Namespaced {
}
