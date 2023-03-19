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
public class Postgresql {
    private String apiVersion = "apiextensions.k8s.io/v1";

    private String kind = "postgresql";

    private ObjectMeta metadata;

    private PostgresqlSpec spec;

    private Status status;
}
