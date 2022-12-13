package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.MYSQL_CLUSTER_API_VERSION;

/**
 * mysql灾备crd
 * @author  liyinlong
 * @since 2021/9/13 4:31 下午
 */
@Data
@Accessors(chain = true)
public class MysqlReplicateCR {

    private String apiVersion = MYSQL_CLUSTER_API_VERSION;

    private String kind;

    private ObjectMeta metadata;

    private MysqlReplicateSpec spec;

    private MysqlReplicateStatus status;

}