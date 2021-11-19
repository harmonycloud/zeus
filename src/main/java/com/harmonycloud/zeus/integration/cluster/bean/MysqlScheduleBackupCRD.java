package com.harmonycloud.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/2 2:36 下午
 */
@Data
@Accessors(chain = true)
public class MysqlScheduleBackupCRD {

    private String apiVersion = "mysql.middleware.harmonycloud.cn/v1alpha1";

    private String kind;

    private ObjectMeta metadata;

    private MysqlScheduleBackupSpec spec;

    private MysqlScheduleBackupStatus status;

}
