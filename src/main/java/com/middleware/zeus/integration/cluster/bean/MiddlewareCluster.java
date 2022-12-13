package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Accessors(chain = true)
@Data
public class MiddlewareCluster {

    private String apiVersion = "harmonycloud.cn/v1";

    private String kind = "MiddlewareCluster";

    private ObjectMeta metadata;

    private MiddlewareClusterSpec spec;

}
