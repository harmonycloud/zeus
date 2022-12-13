package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.MYSQL_CLUSTER_API_VERSION;

/**
 * @author xutianhong
 * @Date 2021/4/2 2:36 下午
 */
@Data
@Accessors(chain = true)
public class MysqlScheduleBackupCR {

    private String apiVersion = MYSQL_CLUSTER_API_VERSION;

    private String kind;

    private ObjectMeta metadata;

    private MysqlScheduleBackupSpec spec;

    private MysqlScheduleBackupStatus status;

}
