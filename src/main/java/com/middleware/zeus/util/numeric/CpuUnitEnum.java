package com.middleware.zeus.util.numeric;

import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;

/**
 * @auther wangpenglei
 * @date 2023/5/6 15:48
 */
public enum CpuUnitEnum {
    ZERO("",new BigDecimal("0")),
    C("",new BigDecimal("1000")),
    DOWN_M("m", new BigDecimal("1")),
    ;
    public String unit;
    public BigDecimal toMilli;

    CpuUnitEnum(String unit, BigDecimal toMilli) {
        this.unit = unit;
        this.toMilli = toMilli;
    }

    public static CpuUnitEnum find(String cpu) {
        if (NumberUtils.isDigits(cpu)) {
            return C;
        }
        for (CpuUnitEnum cpuUnitEnum: CpuUnitEnum.values()) {
            if (cpuUnitEnum.equals(C) || cpuUnitEnum.equals(ZERO)) {
                continue;
            }
            if (cpu.endsWith(cpuUnitEnum.unit)) {
                return cpuUnitEnum;
            }
        }
        return ZERO;
    }

    public static BigDecimal toMilli(String cpu) {
        CpuUnitEnum cpuUnitEnum = find(cpu);
        String data = null;
        for (int i = 0; i < cpu.length(); i++) {
            if (!Character.isDigit(cpu.charAt(i))) {
                data = cpu.substring(0,i);
                break;
            }
        }
        if (data == null) {
            return new BigDecimal(cpu).multiply(C.toMilli);
        } else {
            return new BigDecimal(data).multiply(cpuUnitEnum.toMilli);
        }
    }

    public static BigDecimal milliToUnit(BigDecimal milliNum, String unit){
        if (unit.equals("")) {
            return milliNum.divide(C.toMilli);
        }
        for (CpuUnitEnum cpuUnitEnum: CpuUnitEnum.values()) {
            if (cpuUnitEnum.unit.equals(unit)) {
                return milliNum.divide(cpuUnitEnum.toMilli);
            }
        }
        return new BigDecimal("0");
    }
}
