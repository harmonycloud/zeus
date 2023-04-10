package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.CR_API_VERSION;

/**
 * @author tangtx
 * @date 2021/03/26 11:00 AM
 */
@Accessors(chain = true)
@Data
public class MiddlewareList {

    private String apiVersion = CR_API_VERSION;

    private String kind = "Middleware";

    private ObjectMeta metadata;

    private List<MiddlewareCR> items;
}
