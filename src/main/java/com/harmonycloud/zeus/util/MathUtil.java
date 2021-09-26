package com.harmonycloud.zeus.util;

import java.text.DecimalFormat;

/**
 * @author liyinlong
 * @since 2021/9/23 2:26 下午
 */
public class MathUtil {

    /**
     * 计算百分比
     * @param used
     * @param total
     * @return
     */
    public static String calcPercent(double used, double total) {
        double res = (used / total) * 100;
        DecimalFormat df = new DecimalFormat("######0");
        String percent = df.format(res) + "%";
        return percent;
    }

    /**
     * 计算百分比
     * @param used
     * @param total
     * @return
     */
    public static String calcPercent(int used, int total){
        return calcPercent(Double.parseDouble(String.valueOf(used)), Double.parseDouble(String.valueOf(total)));
    }
}
