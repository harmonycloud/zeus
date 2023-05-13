package com.middleware.zeus.util.numeric;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author liusnze
 * @date 2021/01/06
 */
public class ResourceConvertUtils {

    /**
     * jfrog 存储用量转换
     * 
     * @Author liusenze
     * @Date 2021/1/6 5:49 下午
     * @param usedSpaceStr
     * @return java.lang.Long
     */
    public static Long parseUsedSpace(String usedSpaceStr) {
        String[] strs = usedSpaceStr.split(" ");
        Double space = Double.parseDouble(strs[0].replace(",",""));
        int i = 1;
        switch (strs[1].toUpperCase()) {
            case "KB":
                i *= 1024;
                break;
            case "MB":
                i *= 1024 * 1024;
                break;
            case "GB":
                i *= 1024 * 1024 * 1024;
                break;
            case "TB":
                i *= 1024 * 1024 * 1024 * 1024;
                break;
            case "PB":
                i *= 1024 * 1024 * 1024 * 1024 * 1024;
                break;
            case "EB":
                i *= 1024 * 1024 * 1024 * 1024 * 1024 * 1024;
                break;
            case "ZB":
                i *= 1024 * 1024 * 1024 * 1024 * 1024 * 1024 * 1024;
                break;

        }
        return new BigDecimal(space * i).setScale(0, RoundingMode.UP).longValue();
    }

}
