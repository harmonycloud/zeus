package com.middleware.zeus.common.enums;

/**
 * 中间件备份类型枚举
 * @author liyinlong
 * @since 2021/11/17 9:22 上午
 */
public enum BackupMode {
    SINGLE("single", "单次备份"),
    PERIOD("period", "周期备份")
    ;

    private String mode;

    private String description;

    BackupMode(String type, String description) {
        this.mode = type;
        this.description = description;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
