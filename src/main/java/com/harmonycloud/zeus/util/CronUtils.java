package com.harmonycloud.zeus.util;

import com.middleware.caas.common.enums.DateType;
import com.middleware.tool.date.DateUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author liyinlong
 * @since 2021/9/14 3:12 下午
 */
@Slf4j
public class CronUtils {

    /**
     * 计算下次备份时间
     */
    public static String calculateNextDate(String cron) {
        try {
            String[] crons = cron.split(" ");
            if (crons[4].startsWith(",")) {
                crons[4] = crons[4].substring(1);
            }
            String[] cronWeek = crons[4].split(",");
            List<Date> dateList = new ArrayList<>();
            for (String dayOfWeek : cronWeek) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.MINUTE, Integer.parseInt(crons[0]));
                cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(crons[1]));
                cal.set(Calendar.DAY_OF_WEEK, Integer.parseInt(dayOfWeek) + 1);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Date date = cal.getTime();
                dateList.add(date);
            }
            dateList.sort((d1, d2) -> {
                if (d1.equals(d2)) {
                    return 0;
                }
                return d1.before(d2) ? -1 : 1;
            });
            Date now = new Date();
            for (Date date : dateList) {
                if (now.before(date)) {
                    return DateUtils.dateToString(date, DateType.YYYY_MM_DD_HH_MM_SS.getValue());
                }
            }
            return DateUtils.dateToString(DateUtils.addInteger(dateList.get(0), Calendar.DATE, 7), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
        } catch (Exception e) {
            log.error("定时备份{} ,计算下次备份时间失败", e);
            return null;
        }
    }
    /**
     * 将时间间隔转换为cron表达式
     */
    public static String convertTimeToCron(String time){
        String cron = "*/5 * * * *";
        if (time.contains("m")){
            cron = "*/" + time.split("m")[0] + " * * * *";
        } else if (time.contains("h")){
            cron = "* */" + time.split("m")[0] + " * * *";
        }
        return cron;
    }

    public static String convertCronToTime(String cron){
        String time = "5m";
        String[] crons = cron.split(" ");
        String[] unit = {"m", "h", "d", "M", "y"};
        Pattern pattern = Pattern.compile("^\\*/[1-9][0-9]?$");
        for (int i = 0; i < crons.length; ++i){
            if (pattern.matcher(crons[i]).matches()){
                time = crons[i].split("/")[1] + unit[i];
                break;
            }
        }
        return time;
    }

    public static String parseUtcCron(String cron) {
        String[] newCron = cron.split(" ");
        newCron[1] = getUtcHour(newCron[1]);
        return getCron(newCron);
    }

    public static String parseMysqlUtcCron(String cron) {
        String[] items = cron.split(" ");
        items[1] = getUtcHour(items[1]);
        return getCron(items);
    }

    public static String parseLocalCron(String cron) {
        String[] newCron = cron.split(" ");
        newCron[1] = getLocalHour(newCron[1]);
        return getCron(newCron);
    }

    private static String getCron(String[] items) {
        StringBuffer sbf = new StringBuffer();
        for (int i = 0; i < items.length; i++) {
            if (i != items.length - 1) {
                sbf.append(items[i] + " ");
            } else {
                sbf.append(items[i]);
            }
        }
        return sbf.toString();
    }

    private static String getUtcHour(String hour) {
        int h = Integer.parseInt(hour);
        int temp = h - 8;
        int res = 0;
        if (temp < 0) {
            res = 24 + temp;
        } else {
            res = temp;
        }
        return String.valueOf(res);
    }

    private static String getLocalHour(String hour) {
        int h = Integer.parseInt(hour);
        int temp = h + 8;
        int res = 0;
        if (temp > 23) {
            res = temp - 24;
        } else {
            res = temp;
        }
        return String.valueOf(res);
    }
}
