package com.middleware.zeus.common.enums;

/**
 * @author xutianhong
 * @Date 2021/4/1 11:04 上午
 */
public enum DateType {

    MM_DD("MM-dd"),
    YYYY_MM("yyyy-MM"),
    YYYY_MM_DOT("yyyy.MM"),
    YYYY_MM_DD("yyyy-MM-dd"),
    YYYY_MM_DD_DOT("yyyy.MM.dd"),
    YYYYMMDD_DOT("yyyy.MM.dd"),
    MM_DD_HH_MM("MM-dd HH:mm"),
    MM_DD_HH_MM_SS("MM-dd HH:mm:ss"),
    YYYY_MM_DD_HH_MM("yyyy-MM-dd HH:mm"),
    YYYY_MM_DD_HH_MM_SS("yyyy-MM-dd HH:mm:ss"),
    YYYY_MM_DD_HH_MM_SS_SS("yyyy-MM-dd HH:mm:ss.SS"),

    MM_DD_EN("MM/dd"),
    YYYY_MM_EN("yyyy/MM"),
    YYYY_MM_DD_EN("yyyy/MM/dd"),
    MM_DD_HH_MM_EN("MM/dd HH:mm"),
    MM_DD_HH_MM_SS_EN("MM/dd HH:mm:ss"),
    YYYY_MM_DD_HH_MM_EN("yyyy/MM/dd HH:mm"),
    YYYY_MM_DD_HH_MM_SS_EN("yyyy/MM/dd HH:mm:ss"),
    YYYY_MM_DD_T_HH_MM_SS("yyyy-MM-dd'T'HH:mm:ss"),
    YYYY_MM_DD_T_HH_MM_SS_Z("yyyy-MM-dd'T'HH:mm:ss'Z'"),
    YYYY_MM_DD_T_HH_MM_SS_Z_SSS("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
    YYYY_MM_DD_T_HH_MM_SS_ZONE("yyyy-MM-dd'T'HH:mm:ss'+08:00'"),

    MM_DD_CN("MM月dd日"),
    YYYY_MM_CN("yyyy年MM月"),
    YYYY_MM_DD_CN("yyyy年MM月dd日"),
    MM_DD_HH_MM_CN("MM月dd日 HH:mm"),
    MM_DD_HH_MM_SS_CN("MM月dd日 HH:mm:ss"),
    YYYY_MM_DD_HH_MM_CN("yyyy年MM月dd日 HH:mm"),
    YYYY_MM_DD_HH_MM_SS_CN("yyyy年MM月dd日 HH:mm:ss"),
    EEE_MMM_DD_HH_MM_SS_ZZZ_YYYY("EEE MMM dd HH:mm:ss zzz yyyy"),

    HH_MM("HH:mm"),
    HH_MM_SS("HH:mm:ss"),

    YYYYMMDD("yyyyMMdd"),
    YYYYMMDD_HH_MM_SS("yyyyMMdd HH:mm:ss"),

    YYMMDDHHMMSS("yyyyMMddHHmmss");

    private String value;

    DateType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
