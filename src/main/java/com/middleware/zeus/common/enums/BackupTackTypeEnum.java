package com.middleware.zeus.common.enums;

/**
 * 中间件通用备份类型枚举
 * @author liyinlong
 * @since 2021/11/17 9:22 上午
 */
public enum BackupTackTypeEnum {
    NORMAL(1),
    ACTIVE_ACTIVE(2)
    ;

    private Integer type;

    BackupTackTypeEnum(Integer type) {
        this.type = type;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
