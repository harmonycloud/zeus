package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.HARMONY_CLOUD_CN_V1;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Accessors(chain = true)
@Data
public class MiddlewareCR {

    private String apiVersion = HARMONY_CLOUD_CN_V1;

    private String kind = "Middleware";

    private ObjectMeta metadata;

    private MiddlewareSpec spec;

    private MiddlewareStatus status;

}
