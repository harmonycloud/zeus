package com.harmonycloud.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Accessors(chain = true)
@Data
public class MiddlewareCR {

    private String apiVersion = "harmonycloud.cn/v1";

    private String kind = "Middleware";

    private ObjectMeta metadata;

    private MiddlewareSpec spec;

    private MiddlewareStatus status;

}
