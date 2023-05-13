package com.middleware.zeus.common.enums.middleware;

/**
 * 资源quato单位
 *
 * @author xutianhong
 * @Date 2021/3/29 4:56 下午
 */
public enum ResourceUnitEnum {
    KI("Ki"),
    MI("Mi"),
    GI("Gi"),
    TI("Ti"),
    M("m"),
    K("K"),
    UPPER_M("M"),
    G("G"),
    T("T"),
    ;

    private String unit;

    ResourceUnitEnum(String unit) {
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }
}
