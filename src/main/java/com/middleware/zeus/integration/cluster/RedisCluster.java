package com.middleware.zeus.integration.cluster;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Status;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2023/3/17 5:16 下午
 */
@Accessors(chain = true)
@Data
public class RedisCluster {
    private String apiVersion = "redis.middleware.hc.cn/v1alpha1";

    private String kind = "RedisCluster";

    private ObjectMeta metadata;

    private RedisClusterSpec spec;

    private Status status;
}
