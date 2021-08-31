package com.harmonycloud.zeus.util;

import java.util.Calendar;
import java.util.Date;

/**
 * 获取es索引后缀
 * @author liyinlong
 * @date 2021/7/20 3:28 下午
 */
public class EsIndexUtil {
    public static String getSuffix() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int month = calendar.get(Calendar.MONTH) + 1;
        return calendar.get(Calendar.YEAR) + "." + (month < 10 ? "0" + month : month);
    }
}