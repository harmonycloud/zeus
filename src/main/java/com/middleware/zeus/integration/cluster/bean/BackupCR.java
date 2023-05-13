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
 * @Date 2021/4/6 4:46 下午
 */
@Accessors(chain = true)
@Group(MIDDLEWARE_MYSQL_GROUP)
@Version(MIDDLEWARE_INCLUDE_VERSION)
@Plural(MYSQL_BACKUP)
public class BackupCR extends CustomResource<BackupSpec, BackupStatus> implements Namespaced {

}
