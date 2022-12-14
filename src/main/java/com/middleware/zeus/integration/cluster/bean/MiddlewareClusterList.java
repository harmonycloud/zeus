package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.CR_API_VERSION;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Accessors(chain = true)
@Data
public class MiddlewareClusterList {

    private String apiVersion = CR_API_VERSION;

    private String kind = "MiddlewareCluster";

    private ObjectMeta metadata;

    private List<MiddlewareCluster> items;
}
