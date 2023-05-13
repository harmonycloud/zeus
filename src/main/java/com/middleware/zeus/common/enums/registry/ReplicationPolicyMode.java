package com.middleware.zeus.common.enums.registry;

/**
 * @author chwetion
 * @since 2020/12/20 5:30 下午
 */
public enum ReplicationPolicyMode {
    MANUAL("manual"),
    SCHEDULED("scheduled"),
    ;
    private String type;

    ReplicationPolicyMode(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
