package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;

import java.util.List;

/**
 * @author liyinlong
 * @since 2021/9/15 5:07 下午
 */
@Data
public class MiddlewareRestoreSpec {

    private String name;

    private String type;

    private String backupName;

    private List<RestoreObject> restoreObjects;

    public MiddlewareRestoreSpec() {
    }

}
