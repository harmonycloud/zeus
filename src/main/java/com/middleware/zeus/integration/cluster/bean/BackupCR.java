package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.MYSQL_CLUSTER_API_VERSION;

/**
 * @author xutianhong
 * @Date 2021/4/6 4:46 下午
 */
@Data
@Accessors(chain = true)
public class BackupCR {

    private String apiVersion = MYSQL_CLUSTER_API_VERSION;

    private String kind;

    private ObjectMeta metadata;

    private BackupSpec spec;

    private BackupStatus status;

}
