package com.middleware.zeus.common.enums.registry;


/**
 * @author chwetion
 * @since 2020/12/20 4:21 下午
 */
public enum ReplicationPolicySyncType {
    PULL("pull"),
    PUSH("push"),
    ;

    private String type;

    ReplicationPolicySyncType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
