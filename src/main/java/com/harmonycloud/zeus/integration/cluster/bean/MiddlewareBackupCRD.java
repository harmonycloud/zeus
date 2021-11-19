package com.harmonycloud.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 中间件备份记录crd
 * @author  liyinlong
 * @since 2021/9/13 4:33 下午
 */
@Data
@Accessors(chain = true)
public class MiddlewareBackupCRD {

    private String apiVersion = "harmonycloud.cn/v1";

    private String kind = "MiddlewareBackup";

    private ObjectMeta metadata;

    private MiddlewareBackupSpec spec;

    private MiddlewareBackupStatus status;

}