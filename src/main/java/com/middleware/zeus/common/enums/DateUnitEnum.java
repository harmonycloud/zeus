package com.middleware.zeus.common.enums;

/**
 * @author xutianhong
 * @Date 2021/5/13 2:40 下午
 */
public enum DateUnitEnum {

    SECOND("s"),
    MINUTE("m"),
    HOUR("h"),
    DAY("d"),
    WEEK("w"),
    MONTH("m"),
    YEAR("y")
    ;

    private String unit;

    DateUnitEnum(String unit){
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }

}
