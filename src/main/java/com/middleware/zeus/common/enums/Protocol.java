package com.middleware.zeus.common.enums;

/**
 * @author chwetion
 * @since 2021/1/6 7:02 下午
 */
public enum Protocol {
    TCP("TCP"),
    UDP("UDP"),
    HTTP("HTTP"),
    HTTPS("HTTPS"),
    ;
    private String value;

    Protocol(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
