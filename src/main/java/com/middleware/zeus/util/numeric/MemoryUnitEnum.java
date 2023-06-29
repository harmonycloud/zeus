package com.middleware.zeus.util.numeric;

import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @auther wangpenglei
 * @date 2023/5/6 15:49
 */
@Log4j
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
        if ("0".equals(memory)) {
            return ZERO;
        }
        // 判断是否为纯数字（小数包含在内）
        String pattern = "^[0-9]+(\\.[0-9]+)?$";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(memory);
        if (matcher.matches()) {
            return BYTE;
        }
        for (MemoryUnitEnum memoryUnitEnum: MemoryUnitEnum.values()) {
            // byte和zero没有单位，单独判断
            if (memoryUnitEnum.equals(BYTE) || memoryUnitEnum.equals(ZERO)) {
                continue;
            }
            // 区分大小写的单独判断
            if (DOWN_M.equals(memoryUnitEnum) || M.equals(memoryUnitEnum)
                    || DOWN_K.equals(memoryUnitEnum) || K.equals(memoryUnitEnum)) {
                if (memory.endsWith(memoryUnitEnum.unit) || memory.endsWith(memoryUnitEnum.name)) {
                    return memoryUnitEnum;
                }
            } else {
                if (memory.toLowerCase().endsWith(memoryUnitEnum.unit.toLowerCase())
                    || memory.toLowerCase().endsWith(memoryUnitEnum.name.toLowerCase())) {
                    return memoryUnitEnum;
                }
            }
        }
        // 非法单位转为0
        log.error(memory + "无法识别单位，转换为0");
        return ZERO;
    }

    public static BigDecimal toByte(String memory){
        MemoryUnitEnum memoryUnitEnum = find(memory);
        if (BYTE.equals(memoryUnitEnum)) {
            return new BigDecimal(memory);
        } else if (ZERO.equals(memoryUnitEnum)) {
            // 为0可能是非法字符
            return new BigDecimal("0");
        }
        // 去掉单位
        String pattern = "[A-Za-z]+$";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(memory);
        String data = memory;
        if (matcher.find()) {
            data = memory.substring(0, matcher.start());
        }
        return new BigDecimal(data).multiply(memoryUnitEnum.toByte);
    }

    public static BigDecimal byteToUnit(BigDecimal byteNum, String unit){
        if ("".equals(unit)) {
            return byteNum;
        }
        for(MemoryUnitEnum memoryUnitEnum : MemoryUnitEnum.values()) {
            // zero放在最后判断
            if (memoryUnitEnum.equals(BYTE) || memoryUnitEnum.equals(ZERO)) {
                continue;
            }
            // 先判断大小写有区别的
            if (DOWN_M.unit.equals(unit) || DOWN_M.name.equals(unit)) {
                return byteNum.divide(DOWN_M.toByte);
            } else if (M.unit.equals(unit) || M.name.equals(unit)) {
                return byteNum.divide(M.toByte);
            } else if (K.unit.equals(unit) || K.name.equals(unit)) {
                return byteNum.divide(K.toByte);
            } else if (DOWN_K.unit.equals(unit) || DOWN_K.name.equals(unit)) {
                return byteNum.divide(DOWN_K.toByte);
            } else if (memoryUnitEnum.unit.equalsIgnoreCase(unit) || memoryUnitEnum.name.equalsIgnoreCase(unit)) {
                // 判断可以忽略大小写的
                return byteNum.divide(memoryUnitEnum.toByte);
            }
        }
        // zero和非法单位
        return new BigDecimal("0");
    }
}
