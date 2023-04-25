package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.Data;
import lombok.experimental.Accessors;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.*;

/**
 * @author xutianhong
 * @Date 2021/4/2 2:36 下午
 */
@Accessors(chain = true)
@Group(MIDDLEWARE_MYSQL_GROUP)
@Version(MIDDLEWARE_INCLUDE_VERSION)
@Plural(MYSQL_BACKUP_SCHEDULE)
public class MysqlScheduleBackupCR extends CustomResource<MysqlScheduleBackupSpec, MysqlScheduleBackupStatus> implements Namespaced {

}
