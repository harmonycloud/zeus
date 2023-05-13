package com.middleware.zeus.common.enums;

/**
 * @description 备份服务器用途枚举类
 * @author  liyinlong
 * @since 2023/1/13 2:48 下午
 */
public enum MiddlewareStatusEnum {

    RUNNING(1, "运行中"),
    DELETED(0, "已删除"),
    ;

    private final Integer status;

    private final String phrase;

    MiddlewareStatusEnum(Integer status, String phrase) {
        this.status = status;
        this.phrase = phrase;
    }

    public Integer getStatus() {
        return status;
    }

    public String getPhrase() {
        return phrase;
    }
}
