package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.experimental.Accessors;

import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;

/**
 * @author dengyulong
 * @date 2021/04/02
 */
@Accessors(chain = true)
@Group(MYSQL_CLUSTER_GROUP)
@Version(MYSQL_CLUSTER_VERSION)
@Plural(MYSQL_CLUSTER_PLURAL)
public class MysqlCluster extends CustomResource<MysqlClusterSpec, Status> implements Namespaced {

}
