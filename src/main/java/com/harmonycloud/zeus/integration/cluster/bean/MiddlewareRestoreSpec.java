package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;

/**
 * @author liyinlong
 * @since 2021/9/15 5:07 下午
 */
@Data
public class MiddlewareRestoreSpec {

    private String name;

    private String backupName;

    private String pod;

    public MiddlewareRestoreSpec() {
    }
}
