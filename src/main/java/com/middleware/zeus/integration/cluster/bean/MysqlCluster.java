package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.CR_API_VERSION;

/**
 * @author dengyulong
 * @date 2021/04/02
 */
@Accessors(chain = true)
@Data
public class MysqlCluster {

    private String apiVersion = CR_API_VERSION;

    private String kind = "MysqlCluster";

    private ObjectMeta metadata;

    private MysqlClusterSpec spec;

    private Status status;

}
