package com.middleware.zeus.common.enums.middleware;

/**
 * mysql列操作类型枚举类
 * @author liyinlong
 * @since 2022/11/8 9:39 上午
 */
public enum MysqlOperationEnum {

    ADD("ADD", 1),
    CHANGE("CHANGE", 2),
    MODIFY("MODiFY", 3),
    DROP("DROP", 4),
    NONE("NONE", 5);

    private String action;

    private int code;

    MysqlOperationEnum(String action, int code) {
        this.action = action;
        this.code = code;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
