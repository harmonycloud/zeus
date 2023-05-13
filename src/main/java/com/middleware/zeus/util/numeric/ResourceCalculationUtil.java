package com.middleware.zeus.util.numeric;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author xutianhong
 * @Date 2021/3/29 4:58 下午
 */
public class ResourceCalculationUtil {

    private static Logger logger = LoggerFactory.getLogger(ResourceCalculationUtil.class);

    public static double getResourceValue(String resource, String resourceType, String unit) {
        return getResourceValue(resource, resourceType, unit, 1, RoundingMode.UP);
    }
    /**
     * 从带单位的资源字符串获取指定单位换算的值， 如resource为"1Gi"，unit为Mi，则返回值为1024
     * @param resource 带单位的资源字符串
     * @param resourceType 资源类型，cpu，memory
     * @param unit 转换后的单位
     * @return 按指定单位换算的值
     */
    public static double getResourceValue(String resource, String resourceType, String unit, int decimalCount, RoundingMode roundingMode) {
        switch (resourceType) {
            case "cpu":
                //将resource转换成单位为m的值，然后根据返回单位返回对应的值
//                BigDecimal valueInUnitM = null;
//                if (resource.contains("m")) {
//                    valueInUnitM = new BigDecimal(resource.replace("m", ""));
//                } else {
//                    valueInUnitM = new BigDecimal(resource).multiply(new BigDecimal("1000"));
//                }
//                if ("m".equalsIgnoreCase(unit)) {
//                    return roundNumber(valueInUnitM, decimalCount, roundingMode);
//                } else if ("".equalsIgnoreCase(unit)){
//                    return roundNumber(valueInUnitM.divide(new BigDecimal("1000")), decimalCount, roundingMode);
//                }
                BigDecimal milliResource = CpuUnitEnum.toMilli(resource);
                BigDecimal unitCpuResource = CpuUnitEnum.milliToUnit(milliResource, unit);
                return roundNumber(unitCpuResource, decimalCount, roundingMode);
            case "memory":
            case "disk":
                BigDecimal byteResource = MemoryUnitEnum.toByte(resource);
                BigDecimal unitMemoryResource = MemoryUnitEnum.byteToUnit(byteResource, unit);
                return roundNumber(unitMemoryResource, decimalCount, roundingMode);
//                BigDecimal valueInUnitKi = null;
//                String memory = resource;
//                if (resource.equals("0")) {
//                    return Double.parseDouble(resource);
//                } else if (resource.contains("m")) {
//                    memory = memory.substring(0, memory.indexOf("m"));
//                    valueInUnitKi = new BigDecimal(memory).divide(new BigDecimal("1000")).divide(new BigDecimal("1024"));
//                } else if (NumberUtils.isDigits(resource)) {
//                    // 没有单位是字节，除以1024为Ki
//                    valueInUnitKi = new BigDecimal(memory).divide(new BigDecimal("1024"));
//                } else if (resource.contains("Ki")) {
//                    memory = memory.substring(0, memory.indexOf("Ki"));
//                    valueInUnitKi = new BigDecimal(memory);
//                } else if (memory.contains("k")){
//                    memory = memory.substring(0, memory.indexOf("k"));
//                    valueInUnitKi = new BigDecimal(memory);
//                } else if (resource.contains("K")){
//                    memory = memory.substring(0, memory.indexOf("K"));
//                    valueInUnitKi = new BigDecimal(memory).multiply(new BigDecimal("1000")).divide(new BigDecimal("1024"));
//                } else if (memory.contains("Mi")) {
//                    memory = memory.substring(0, memory.indexOf("Mi"));
//                    valueInUnitKi = new BigDecimal(memory).multiply(new BigDecimal("1024"));
//                } else if (resource.contains("M")){
//                    memory = memory.substring(0, memory.indexOf("M"));
//                    valueInUnitKi = new BigDecimal(memory).multiply(new BigDecimal("1000")).multiply(new BigDecimal("1000")).divide(new BigDecimal("1024"));
//                } else if (memory.contains("Gi")) {
//                    memory = memory.substring(0, memory.indexOf("Gi"));
//                    valueInUnitKi = new BigDecimal(memory).multiply(new BigDecimal("1024")).multiply(new BigDecimal("1024"));
//                } else if (resource.contains("G")){
//                    memory = memory.substring(0, memory.indexOf("G"));
//                    valueInUnitKi = new BigDecimal(memory).multiply(new BigDecimal("1000")).multiply(new BigDecimal("1000")).multiply(new BigDecimal("1000")).divide(new BigDecimal("1024"));
//                }else if (memory.contains("Ti")) {
//                    memory = memory.substring(0, memory.indexOf("Ti"));
//                    valueInUnitKi = new BigDecimal(memory).multiply(new BigDecimal("1024")).multiply(new BigDecimal("1024")).multiply(new BigDecimal("1024"));
//                } else if (memory.contains("T")) {
//                    memory = memory.substring(0, memory.indexOf("T"));
//                    valueInUnitKi = new BigDecimal(memory).multiply(new BigDecimal("1000")).multiply(new BigDecimal("1000")).multiply(new BigDecimal("1000")).multiply(new BigDecimal("1000")).divide(new BigDecimal("1024"));
//                }
//                BigDecimal result = new BigDecimal("0");
//                BigDecimal byteValue = valueInUnitKi.multiply(new BigDecimal("1024"));
//                if ("m".equals(unit)) {
//                    result = byteValue.multiply(new BigDecimal("1000"));
//                } else if ("M".equals(unit)) {
//                    result = byteValue.divide(new BigDecimal("1000").pow(2));
//                } else if ("k".equals(unit) || "Ki".equalsIgnoreCase(unit)) {
//                    result = byteValue.divide(new BigDecimal("1024").pow(1));
//                } else if ("K".equals(unit)) {
//                    result = byteValue.divide(new BigDecimal("1000").pow(1));
//                } else if ("Mi".equalsIgnoreCase(unit)) {
//                    result = byteValue.divide(new BigDecimal("1024").pow(2));
//                } else if ("Gi".equalsIgnoreCase(unit)) {
//                    result = byteValue.divide(new BigDecimal("1024").pow(3));
//                } else if ("G".equalsIgnoreCase(unit)) {
//                    result = byteValue.divide(new BigDecimal("1000").pow(3));
//                } else if ("Ti".equalsIgnoreCase(unit)) {
//                    result = byteValue.divide(new BigDecimal("1024").pow(4));
//                } else if ("T".equalsIgnoreCase(unit)) {
//                    result = byteValue.divide(new BigDecimal("1000").pow(4));
//                } else if ("".equalsIgnoreCase(unit)) {
//                    result = byteValue;
//                }
//                return roundNumber(result,decimalCount,roundingMode);
            default:
                return 0;
        }


    }

    public static double roundNumber2TwoDecimalWithCeiling(Double value){
        return roundNumber(BigDecimal.valueOf(value), 2, RoundingMode.CEILING);
    }

    public static double roundNumber(BigDecimal value, int decimalCount, RoundingMode roundingMode) {
        BigDecimal bigDecimal = value.setScale(decimalCount, roundingMode);
        return bigDecimal.doubleValue();
    }

    /**
     * 对k8s yaml中的 resource的单位Ki,m或无单位的memory进行转换成Mi
     */
    public static String convertMemoryUnit(String sourceMemory){

        if (StringUtils.isBlank(sourceMemory) || sourceMemory.equals("0")) {
            return sourceMemory;
        }
        //没有单位为字节单位，转换成m计算
        if (Character.isDigit(sourceMemory.charAt(sourceMemory.length()-1))) {
            double value = getResourceValue(new BigDecimal(sourceMemory).multiply(new BigDecimal("1000")) + "m", "memory", "Mi", 0, RoundingMode.UP);
            return Double.valueOf(value).intValue() + "Mi";
        }
        if (sourceMemory.endsWith("Ki") || sourceMemory.endsWith("m")) {
            double value = getResourceValue(sourceMemory , "memory", "Mi", 0, RoundingMode.UP);
            return Double.valueOf(value).intValue() + "Mi";
        }
        return sourceMemory;
    }

    /**
     * 内存单位转换成MB(统一单位)
     *
     * @param memory 内存
     * @param unit   单位
     * @return
     */
    public static double transformMemoryToMb(double memory, String unit) {
        return transformMemoryToMb(memory, unit, 1, RoundingMode.HALF_DOWN);
    }

    /**
     * 内存单位转换成MB(统一单位)
     *
     * @param memory       内存
     * @param unit         单位
     * @param roundingMode 保留方式
     * @return
     */
    public static double transformMemoryToMb(double memory, String unit, RoundingMode roundingMode) {
        return transformMemoryToMb(memory, unit, 1, roundingMode);
    }

    /**
     * 内存单位转换成MB(统一单位)
     *
     * @param memory       内存
     * @param unit         单位
     * @param scale        保留小数
     * @param roundingMode 保留方式
     * @return
     */
    public static double transformMemoryToMb(double memory, String unit, int scale, RoundingMode roundingMode) {
        // 判断单位
        double memoryMb = memory;
        switch (unit) {
            case "KB":
            case "Ki":
                memoryMb = memory / 1024;
                break;
            case "MB":
            case "Mi":
                memoryMb = memory;
                break;
            case "GB":
            case "Gi":
                memoryMb = memory * 1024;
                break;
            case "TB":
            case "Ti":
                memoryMb = memory * 1024 * 1024;
                break;
            case "PB":
            case "Pi":
                memoryMb = memory * 1024 * 1024
                        * 1024;
                break;
            default:
        }
        return BigDecimal.valueOf(memoryMb).setScale(scale, roundingMode).doubleValue();
    }

    /**
     * 内存转换成GB，四舍五入
     *
     * @param memory 内存
     * @param unit   单位
     * @return
     */
    public static double transformMemoryToGb(double memory, String unit) {
        return transformMemoryToGb(memory, unit, 1, RoundingMode.HALF_UP);
    }

    /**
     * 内存转换成GB
     *
     * @param memory       内存
     * @param unit         单位
     * @param roundingMode 舍入模式
     * @return
     */
    public static double transformMemoryToGb(double memory, String unit, RoundingMode roundingMode) {
        return transformMemoryToGb(memory, unit, 1, roundingMode);
    }

    /**
     * 内存转换成GB
     *
     * @param memory       内存
     * @param unit         单位
     * @param scale        保留小数
     * @param roundingMode 舍入模式
     * @return
     */
    public static double transformMemoryToGb(double memory, String unit, int scale, RoundingMode roundingMode) {
        // 判断单位
        double memoryMb = memory;
        switch (unit) {
            case "Ki":
            case "KB":
                memoryMb = memory / (1024 * 1024);
                break;
            case "Mi":
            case "MB":
                memoryMb = memory / 1024;
                break;
            case "Gi":
            case "GB":
                memoryMb = memory;
                break;
            case "Ti":
            case "TB":
                memoryMb = memory * 1024;
                break;
            case "Pi":
            case "PB":
                memoryMb = memory * 1024 * 1024;
                break;
            default:
        }
        // bigDecimal的构造方法，传入double可能会有精度问题，比如0.3被表示为0.2999
        // 使用传入String的构造方法则可以避免该问题
        return BigDecimal.valueOf(memoryMb).setScale(scale, roundingMode).doubleValue();
    }

}
