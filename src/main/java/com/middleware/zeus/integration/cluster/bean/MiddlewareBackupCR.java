package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.CR_API_VERSION;

/**
 * 中间件备份记录crd
 * @author  liyinlong
 * @since 2021/9/13 4:33 下午
 */
@Data
@Accessors(chain = true)
public class MiddlewareBackupCR {

    private String apiVersion = CR_API_VERSION;

    private String kind = "MiddlewareBackup";

    private ObjectMeta metadata;

    private MiddlewareBackupSpec spec;

    private MiddlewareBackupStatus status;

}