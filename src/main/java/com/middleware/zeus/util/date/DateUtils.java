package com.middleware.zeus.util.date;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * @author chwetion
 * @since 2020/12/16 4:24 下午
 */
public class DateUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateUtils.class);

    public final static String YYYY_MM_DD_T_HH_MM_SS_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public final static String YYYY_MM_DD_T_HH_MM_SS_SSS_Z = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    public static final String YYYY_MM_DD = "yyyy-MM-dd";

    public static long convertUTCStr2Num(String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS_Z);
        return sdf.parse(date).getTime();
    }

    public static Date parseUTCDate(String date) {
        return parseDate(date, YYYY_MM_DD_T_HH_MM_SS_Z, TimeZone.getTimeZone("GMT"));
    }

    public static Date parseUTCSDate(String date) {
        return parseDate(date, YYYY_MM_DD_T_HH_MM_SS_SSS_Z, TimeZone.getTimeZone("GMT"));
    }

    public static boolean isUTCStrNew(String target, String other) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS_Z);
        return sdf.parse(target).getTime() > sdf.parse(other).getTime();
    }

    public static String dateToString(Date date, String pattern) {
        String dateString = null;
        if (date != null) {
            try {
                dateString = new SimpleDateFormat(pattern).format(date);
            } catch (Exception e) {
            }
        }
        return dateString;
    }

    /**
     * 将日期字符串转化为日期。失败返回null。
     *
     * @param date
     *            日期字符串
     * @param pattern
     *            日期格式
     * @return 日期
     */
    public static Date parseDate(String date, String pattern) {
        Date myDate = null;
        if (date != null) {
            try {
                myDate = new SimpleDateFormat(pattern).parse(date);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return myDate;
    }

    /**
     * 格式化Date时间为指定时区
     *
     * @param time
     *            Long类型时间戳
     * @param timeFormat
     *            String类型格式
     * @return 格式化后的字符串
     */
    public static String formatDate(Long time, String timeFormat, TimeZone timeZone) {
        if (time == null) {
            return null;
        }
        DateFormat dateFormat = new SimpleDateFormat(timeFormat);
        if (null != timeZone) {
            dateFormat.setTimeZone(timeZone);
        }
        return dateFormat.format(time);
    }

    /**
     * 格式化Date时间为指定时区
     *
     * @param time
     *            字符串时间
     * @param timeFormat
     *            String类型格式
     * @param timeZone
     *            时区
     * @return 格式化后的字符串
     */
    public static Date parseDate(String time, String timeFormat, TimeZone timeZone) {
        if (StringUtils.isBlank(time)) {
            return null;
        }
        DateFormat dateFormat = new SimpleDateFormat(timeFormat);
        if (null != timeZone) {
            dateFormat.setTimeZone(timeZone);
        }
        try {
            return dateFormat.parse(time);
        } catch (ParseException e) {
            LOGGER.error("Unparseable date: {}", time);
        }
        return null;
    }

    /**
     * 获取日期的年份。失败返回0。
     *
     * @param date
     *            日期
     * @return 年份
     */
    public static int getYear(Date date){
        return getInteger(date, Calendar.YEAR);
    }

    /**
     * 获取日期的月份。失败返回0。
     *
     * @param date
     *            日期
     * @return 月份
     */
    public static int getMonth(Date date){
        return getInteger(date, Calendar.MONTH);
    }

    /**
     * 获取日期的天。失败返回0。
     *
     * @param date
     *            日期
     * @return 月份
     */
    public static int getDay(Date date){
        return getInteger(date, Calendar.DAY_OF_MONTH);
    }

    /**
     * 获取日期的小时。失败返回0。
     *
     * @param date
     *            日期
     * @return 小时
     */
    public static int getHour(Date date) {
        return getInteger(date, Calendar.HOUR_OF_DAY);
    }

    /**
     * 获取日期的分钟。失败返回0。
     *
     * @param date
     *            日期
     * @return 分钟
     */
    public static int getMinute(Date date) {
        return getInteger(date, Calendar.MINUTE);
    }

    /**
     * 获取日期中的某数值。如获取月份
     *
     * @param date
     *            日期
     * @param dateType
     *            日期格式
     * @return 数值
     */
    private static int getInteger(Date date, int dateType) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(dateType);
    }

    /**
     * 增加日期中某类型的某数值。如增加日期
     *
     * @param date
     *            日期
     * @param dateType
     *            类型
     * @param amount
     *            数值
     * @return 计算后日期
     */
    public static Date addInteger(Date date, int dateType, int amount) {
        Date myDate = null;
        if (date != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(dateType, amount);
            myDate = calendar.getTime();
        }
        return myDate;
    }

    /**
     * @param date
     *            日期
     * @param otherDate
     *            另一个日期
     * @return 相差秒数
     */
    public static long getIntervalDays(Date date, Date otherDate){
        long time = Math.abs(date.getTime() - otherDate.getTime());
        return TimeUnit.MILLISECONDS.toSeconds(time);
    }

    /**
     * 根据日期获取星期
     *
     * @param date
     * @return string
     */
    public static String getWeekFromDate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int week = c.get(Calendar.DAY_OF_WEEK);
        switch (week) {
            case Calendar.SUNDAY:
                return "7";
            case Calendar.MONDAY:
                return "1";
            case Calendar.TUESDAY:
                return "2";
            case Calendar.WEDNESDAY:
                return "3";
            case Calendar.THURSDAY:
                return "4";
            case Calendar.FRIDAY:
                return "5";
            case Calendar.SATURDAY:
                return "6";
            default:
                return "";
        }
    }

    /**
     * 将日期转化为日期字符串。失败返回null。
     *
     * @param date     日期
     * @param parttern 日期格式
     * @return 日期字符串
     */
    public static String DateToString(Date date, String parttern) {
        String dateString = null;
        if (date != null) {
            try {
                dateString = getDateFormat(parttern).format(date);
            } catch (Exception e) {
            }
        }
        return dateString;
    }

    /**
     * 获取SimpleDateFormat
     *
     * @param parttern 日期格式
     * @return SimpleDateFormat对象
     * @throws RuntimeException 异常：非法日期格式
     */
    private static SimpleDateFormat getDateFormat(String parttern) throws RuntimeException {
        return new SimpleDateFormat(parttern);
    }

    public static Date getCurrentUtcTime() {

        SimpleDateFormat adf = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS_Z);

        StringBuffer UTCTimeBuffer = new StringBuffer();
        // 1、取得本地时间：
        Calendar cal = Calendar.getInstance();
        // 2、取得时间偏移量：
        int zoneOffset = cal.get(java.util.Calendar.ZONE_OFFSET);
        // 3、取得夏令时差：
        int dstOffset = cal.get(java.util.Calendar.DST_OFFSET);
        // 4、从本地时间里扣除这些差量，即可以取得UTC时间：
        cal.add(java.util.Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        UTCTimeBuffer.append(year).append("-").append(month).append("-").append(day);
        UTCTimeBuffer.append("T").append(hour).append(":").append(minute).append(":").append(second).append("Z");
        Date date = null;
        try {
            date = adf.parse(UTCTimeBuffer.toString());
        } catch (ParseException e) {
            LOGGER.warn("获取CurrentUtcTime失败", e);
        }
        return date;
    }

    // 获取指定日期的起始时间（00:00:00）
    public static Date getStartOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    // 获取指定日期的结束时间（23:59:59）
    public static Date getEndOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

}
