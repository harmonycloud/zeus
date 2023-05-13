package com.middleware.zeus.integration.cluster.bean;

import com.middleware.zeus.common.constants.PostgresqlConstant;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2023/3/17 5:16 下午
 */
@AllArgsConstructor
@Accessors(chain = true)
@Group(PostgresqlConstant.POSTGRESQL_GROUP)
@Version(PostgresqlConstant.POSTGRESQL_VERSION)
@Plural(PostgresqlConstant.POSTGRESQL_PLURAL)
public class Postgresql extends CustomResource<PostgresqlSpec, Status> implements Namespaced {

}
