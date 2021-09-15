package com.harmonycloud.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 中间件备份crd
 * @author  liyinlong
 * @since 2021/9/13 4:33 下午
 */
@Data
@Accessors(chain = true)
public class MiddlewareBackupScheduleCRD {

    private String apiVersion = "harmonycloud.cn/v1";

    private String kind;

    private ObjectMeta metadata;

    private MiddlewareBackupScheduleSpec spec;

    private MiddlewareBackupScheduleStatus status;

}