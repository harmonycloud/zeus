package com.middleware.zeus.common.enums.registry;

/**
 * @author chwetion
 * @since 2021/1/8 10:52 上午
 */
public enum RegistryPolicyType {
    HOURLY("Hourly"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    CUSTOM("Custom"),
    MANUAL("Manual"),
    NONE("None"),
    ;
    private String harborType;

    RegistryPolicyType(String harborType) {
        this.harborType = harborType;
    }

    public String getHarborType() {
        return harborType;
    }
}
