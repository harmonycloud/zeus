package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.AllArgsConstructor;

import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;

/**
 * @author liyinlong
 * @since 2021/9/15 5:04 下午
 */
@AllArgsConstructor
@Group(CR_GROUP)
@Version(V1)
@Plural(MIDDLEWARERESTORES)
@Kind("MiddlewareRestore")
public class MiddlewareRestoreCR extends CustomResource<MiddlewareRestoreSpec, MiddlewareRestoreStatus> implements Namespaced {

}
