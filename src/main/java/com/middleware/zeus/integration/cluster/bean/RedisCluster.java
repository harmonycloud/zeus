package com.middleware.zeus.integration.cluster.bean;

import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.V1_ALPHA1;

import com.middleware.zeus.common.constants.RedisConstant;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2023/3/17 5:16 下午
 */
@Accessors(chain = true)
@Group(RedisConstant.REDISCLUSTER_GROUP)
@Version(V1_ALPHA1)
@Plural(RedisConstant.REDISCLUSTER_PLURAL)
public class RedisCluster extends CustomResource<RedisClusterSpec, Status> implements Namespaced {

}
