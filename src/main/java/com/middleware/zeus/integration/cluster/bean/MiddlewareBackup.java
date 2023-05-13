package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;

/**
 * 中间件备份记录crd
 * @author  liyinlong
 * @since 2021/9/13 4:33 下午
 */
@AllArgsConstructor
@Accessors(chain = true)
@Group(CR_GROUP)
@Version(V1)
@Plural(MIDDLEWAREBACKUP)
public class MiddlewareBackup extends CustomResource<MiddlewareBackupSpec, MiddlewareBackupStatus> implements Namespaced {

}