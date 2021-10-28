package com.harmonycloud.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * mysql灾备crd
 * @author  liyinlong
 * @since 2021/9/13 4:31 下午
 */
@Data
@Accessors(chain = true)
public class MysqlReplicateCRD {

    private String apiVersion = "mysql.middleware.harmonycloud.cn/v1alpha1";

    private String kind;

    private ObjectMeta metadata;

    private MysqlReplicateSpec spec;

    private MysqlReplicateStatus status;

}