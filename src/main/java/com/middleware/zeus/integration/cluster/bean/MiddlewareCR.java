package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.experimental.Accessors;

import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Accessors(chain = true)
@Version(MIDDLEWARE_CLUSTER_VERSION)
@Group(MIDDLEWARE_CLUSTER_GROUP)
@Plural(MIDDLEWARE_PLURAL)
@Kind(MIDDLEWARE_KIND)
public class MiddlewareCR extends CustomResource<MiddlewareSpec, MiddlewareStatus> implements Namespaced {


}
