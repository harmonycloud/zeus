package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;

/**
 * @author liyinlong
 * @since 2021/9/15 5:04 下午
 */
@Data
public class MiddlewareRestoreCR {

    private String apiVersion = "harmonycloud.cn/v1";

    private String kind = "MiddlewareRestore";

    private ObjectMeta metadata;

    private MiddlewareRestoreSpec spec;

    private MiddlewareRestoreStatus status;

    public MiddlewareRestoreCR() {
    }

    public MiddlewareRestoreCR(String backupName, String middlewareName) {
        this.spec = spec;
    }
}
