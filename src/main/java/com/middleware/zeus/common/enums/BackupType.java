package com.middleware.zeus.common.enums;

/**
 * 中间件通用备份类型枚举
 * @author liyinlong
 * @since 2021/11/17 9:22 上午
 */
public enum BackupType {
    CLUSTER("Cluster"),
    POD("Pod")
    ;

    private String type;

    BackupType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
