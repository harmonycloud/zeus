package com.harmonycloud.zeus.util;

import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.tool.date.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTimeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

    public static String parseUtcCron(String cron) {
        String newCron = cron.replaceAll("[?]", "*");
        String[] items = newCron.split(" ");
        items[1] = getUtcHour(items[1]);
        return getCron(items);
    }

    public static String parseLocalCron(String cron) {
        String newCron = cron.replaceAll("[*]", "?");
        String[] items = newCron.split(" ");
        items[1] = getLocalHour(items[1]);
        return getCron(items);
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
