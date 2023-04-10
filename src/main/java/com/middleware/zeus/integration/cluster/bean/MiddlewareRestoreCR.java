package com.middleware.zeus.integration.cluster.bean;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.CR_API_VERSION;

/**
 * @author liyinlong
 * @since 2021/9/15 5:04 下午
 */
@Data
public class MiddlewareRestoreCR {

    private String apiVersion = CR_API_VERSION;

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
