package com.middleware.zeus.util;

import com.middleware.tool.numeric.ResourceCalculationUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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
        if (used == 0 || total == 0) {
            return "";
        }
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

    /**
     * 连续数字数组分组 1,2,4,8,9,11 -> [1-2],[4],[8-9],[11]
     * @param list
     * @return
     */
    public static String convert(List<Integer> list) {
        list.sort(Comparator.comparing(Integer::intValue));
        int state = 0;
        String result = "";
        for (int i = 0; i < list.size(); i++) {
            if (i == list.size() - 1) {
                state = 2;
            }
            if (state == 0) {
                if (list.get(i + 1) == list.get(i) + 1) {
                    result += Integer.toString(list.get(i));
                    result += "-";
                    state = 1;
                } else {
                    result += Integer.toString(list.get(i));
                    result += ",";
                }
            } else if (state == 1) {
                if (list.get(i + 1) != list.get(i) + 1) {
                    result += Integer.toString(list.get(i));
                    result += ",";
                    state = 0;
                } else {
                    result += list.get(i) + "-";
                }
            } else {
                result += Integer.toString(list.get(i));
            }
        }
        String[] str = result.split(",");

        StringBuffer sbf = new StringBuffer();
        for (int stritem = 0; stritem < str.length; stritem++) {
            String[] sp = str[stritem].split("-");
            List<String> tt = Arrays.asList(sp);
            if (tt.size() == 1) {
                sbf.append(Arrays.toString(tt.toArray()));
            } else {
                sbf.append("[" + tt.get(0) + "—" + tt.get(tt.size() - 1) + "]");
            }
            if (stritem != str.length - 1) {
                sbf.append(",");
            }
        }
        return sbf.toString();
    }

    /**
     * 判断一个字符串是否是数字
     * @param s
     * @return
     */
    public static boolean isDigit(String s) {
        if (s == null) {
            return false;
        }
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断一个字符串是否是数字
     * @param x
     * @param y
     * @param size
     * @return double
     */
    public static double multiplyExact(double x, double y, int size){
        double r = x * y;
        if (size > 0){
            ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(r), size, RoundingMode.CEILING);
        }
        return r;
    }

}
