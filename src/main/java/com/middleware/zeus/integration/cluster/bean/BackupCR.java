package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/6 4:46 下午
 */
@Data
@Accessors(chain = true)
public class BackupCR {

    private String apiVersion = "mysql.middleware.harmonycloud.cn/v1alpha1";

    private String kind;

    private ObjectMeta metadata;

    private BackupSpec spec;

    private BackupStatus status;

}
