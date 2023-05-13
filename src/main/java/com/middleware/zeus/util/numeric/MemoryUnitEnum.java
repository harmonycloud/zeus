package com.middleware.zeus.util.numeric;

import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;

/**
 * @auther wangpenglei
 * @date 2023/5/6 15:49
 */
public enum MemoryUnitEnum {
    ZERO("", new BigDecimal(0), ""),
    DOWN_M("m", new BigDecimal("0.001"), "m"),
    BYTE("",new BigDecimal("1"), "Byte"),
    DOWN_K("k", new BigDecimal("1024"), "KB"),
    K("K", new BigDecimal("1000"), "K"),
    KI("Ki", new BigDecimal("1024"), "KB"),
    M("M", new BigDecimal("1000").pow(2), "M"),
    MI("Mi", new BigDecimal("1024").pow(2), "MB"),
    G("G", new BigDecimal("1000").pow(3), "G"),
    GI("Gi", new BigDecimal("1024").pow(3), "GB"),
    T("T", new BigDecimal("1000").pow(4), "T"),
    TI("Ti", new BigDecimal("1024").pow(4), "TB"),
    ;
    public String unit;
    public BigDecimal toByte;
    public String name;

    MemoryUnitEnum(String unit, BigDecimal toByte, String name) {
        this.unit = unit;
        this.toByte = toByte;
        this.name = name;
    }

    public static MemoryUnitEnum find(String memory) {
        if (memory.equals("0")) {
            return ZERO;
        }
        if (NumberUtils.isDigits(memory)) {
            return BYTE;
        }
        for (MemoryUnitEnum memoryUnitEnum: MemoryUnitEnum.values()) {
            if (memoryUnitEnum.equals(BYTE) || memoryUnitEnum.equals(ZERO)) {
                continue;
            }
            if (memory.endsWith(memoryUnitEnum.unit) || memory.endsWith(memoryUnitEnum.name)) {
                return memoryUnitEnum;
            }
        }
        return ZERO;
    }

    public static BigDecimal toByte(String memory){
        MemoryUnitEnum memoryUnitEnum = find(memory);
        String data = null;
        for (int i = 0; i < memory.length(); i++) {
            if (!Character.isDigit(memory.charAt(i))) {
                data = memory.substring(0,i);
                break;
            }
        }
        if (data == null) {
            return new BigDecimal(memory);
        } else {
            return new BigDecimal(data).multiply(memoryUnitEnum.toByte);
        }
    }

    public static BigDecimal byteToUnit(BigDecimal byteNum, String unit){
        if (unit.equals("")) {
            return byteNum;
        }
        for(MemoryUnitEnum memoryUnitEnum : MemoryUnitEnum.values()) {
            if (memoryUnitEnum.equals(BYTE) || memoryUnitEnum.equals(ZERO)) {
                continue;
            }
            if (unit.equals("m")) {
                return byteNum.divide(DOWN_M.toByte);
            } else if (unit.equals("M")) {
                return byteNum.divide(M.toByte);
            } else if (unit.equals("K")) {
                return byteNum.divide(K.toByte);
            } else if (unit.equals("k")) {
                return byteNum.divide(DOWN_K.toByte);
            } else if (unit.equalsIgnoreCase(memoryUnitEnum.unit)) {
                return byteNum.divide(memoryUnitEnum.toByte);
            }
        }
        return new BigDecimal("0");
    }
}
