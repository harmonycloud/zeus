package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.HARMONY_CLOUD_CN_V1;

/**
 * 中间件备份记录crd
 * @author  liyinlong
 * @since 2021/9/13 4:33 下午
 */
@Data
@Accessors(chain = true)
public class MiddlewareBackupCR {

    private String apiVersion = HARMONY_CLOUD_CN_V1;

    private String kind = "MiddlewareBackup";

    private ObjectMeta metadata;

    private MiddlewareBackupSpec spec;

    private MiddlewareBackupStatus status;

}