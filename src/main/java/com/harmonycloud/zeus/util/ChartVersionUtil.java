package com.harmonycloud.zeus.util;

/**
 * @author liyinlong
 * @since 2022/2/25 3:01 下午
 */
public class ChartVersionUtil {

    public static int compare(String chartVersion1, String chartVersion2) {
        if (chartVersion1.equals(chartVersion2)) {
            return 0;
        }
        //根据 "-" 进行分隔
        String[] tempO1 = chartVersion1.split("-");
        String[] tempO2 = chartVersion2.split("-");
        //根据 "." 进行分隔
        String[] numO1 = tempO1[0].split("\\.");
        String[] numO2 = tempO2[0].split("\\.");
        // 先判断"-"前 如：2.3.4 和 1.5.6
        int size = Math.min(numO1.length, numO2.length);
        for (int i = 0; i < size; ++i) {
            int intO1 = Integer.parseInt(numO1[i]);
            int intO2 = Integer.parseInt(numO2[i]);
            if (intO1 == intO2) {
                continue;
            }
            return Integer.compare(intO2, intO1);
        }
        // 判断3.0和3.0.1
        if (numO1.length != numO2.length) {
            return Integer.compare(numO2.length, numO1.length);
        }
        // 判断1.5.6和1.5.6-1
        if (tempO1.length != tempO2.length) {
            return Integer.compare(tempO2.length, tempO1.length);
        }
        // 判断1.5.6-2和1.5.6-3
        return tempO2[1].compareTo(tempO1[1]);
    }
}
