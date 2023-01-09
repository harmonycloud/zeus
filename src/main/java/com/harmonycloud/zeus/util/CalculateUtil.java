package com.harmonycloud.zeus.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author xutianhong
 * @Date 2023/1/6 4:22 下午
 */
public class CalculateUtil {

    public static Double division(Double d1, Double d2, int decimalCount) {
        return BigDecimal.valueOf(d1).divide(BigDecimal.valueOf(d2), decimalCount, RoundingMode.CEILING).doubleValue();
    }

}
