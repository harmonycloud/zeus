package com.middleware.zeus.common.enums.registry;

/**
 * @author chwetion
 * @since 2021/1/8 10:20 上午
 */
public enum RegistryGCTaskStatus {
    PENDING("pending"),
    RUNNING("running"),
    ERROR("error"),
    STOPPED("stopped"),
    FINISHED("finished"),
    CANCELED("canceled"),
    RETRYING("retrying"),
    CONTINUE("_continue"),
    SCHEDULED("scheduled"),
    ;

    private String harborStatus;

    RegistryGCTaskStatus(String harborStatus) {
        this.harborStatus = harborStatus;
    }

    public String getHarborStatus() {
        return harborStatus;
    }
}
