package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2021/9/15 5:07 下午
 */
@Data
public class MiddlewareRestoreSpec {

    private String name;

    private String backupName;

    private String type;

    private List<String> pods;

    private Map<String,String> restoreBind;

    public MiddlewareRestoreSpec() {
    }
}
