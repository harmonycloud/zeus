package com.harmonycloud.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2021/04/02
 */
@Accessors(chain = true)
@Data
public class MysqlCluster {

    private String apiVersion = "harmonycloud.cn/v1";

    private String kind = "MysqlCluster";

    private ObjectMeta metadata;

    private MysqlClusterSpec spec;

    private Status status;

}
