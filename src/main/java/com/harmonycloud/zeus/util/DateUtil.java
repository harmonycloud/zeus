package com.harmonycloud.zeus.util;

import com.middleware.caas.common.constants.DateStyle;
import com.middleware.caas.common.enums.DateType;
import com.middleware.tool.date.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.middleware.caas.common.constants.CommonConstant.NUM_NINE;
import static com.middleware.caas.common.constants.CommonConstant.NUM_TEN;

public class DateUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateUtil.class);
    /**
     * 通用的一个DateFormat
     */
    public final static SimpleDateFormat commonDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 处理时分秒的DateFormat
     */
    public final static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 不显示秒的DateFormat
     */
    public final static SimpleDateFormat noSecondFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    /**
     * utc时间格式
     */
    public final static SimpleDateFormat UTC_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public final static int MILLISECONDS_OF_SECOND = 1000;
    public final static int MILLISECONDS_OF_MINUTE = 60000;
    public final static int MILLISECONDS_OF_HOUR = 3600000;
    public final static int MILLISECONDS_OF_DAY = 86400000;
    public final static int SECONDS_OF_MINUTE = 60;
    public final static int MINUTES_OF_HOUR = 60;
    public final static int HOURS_OF_DAY = 24;

    public final static String TIME_UNIT_SECOND = "s";
    public final static String TIME_UNIT_MINUTE = "m";
    public final static String TIME_UNIT_HOUR= "h";
    public final static String TIME_UNIT_DAY = "d";

    public static Date getCurrentUtcTime() {

        SimpleDateFormat adf = new SimpleDateFormat(DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue());

        StringBuffer UTCTimeBuffer = new StringBuffer();
        // 1、取得本地时间：
        Calendar cal = Calendar.getInstance();
        // 2、取得时间偏移量：
        int zoneOffset = cal.get(Calendar.ZONE_OFFSET);
        // 3、取得夏令时差：
        int dstOffset = cal.get(Calendar.DST_OFFSET);
        // 4、从本地时间里扣除这些差量，即可以取得UTC时间：
        cal.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));
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
            // TODO Auto-generated catch block
            LOGGER.warn("获取CurrentUtcTime失败", e);
        }
        return date;
    }

    /**
     * 获取当前utc时区格式的时间字符串
     * @return
     */
    public static String getCurrentUtcTimestamp() {
        Date currentTime = DateUtil.getCurrentUtcTime();
        return DateUtil.UTC_FORMAT.format(currentTime);
    }
    /**
     * 获取SimpleDateFormat
     * 
     * @param parttern
     *            日期格式
     * @return SimpleDateFormat对象
     * @throws RuntimeException
     *             异常：非法日期格式
     */
    private static SimpleDateFormat getDateFormat(String parttern) throws RuntimeException{
        return new SimpleDateFormat(parttern);
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
    private static int getInteger(Date date, int dateType){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(dateType);
    }

    /**
     * 增加日期中某类型的某数值。如增加日期
     * 
     * @param date
     *            日期字符串
     * @param dateType
     *            类型
     * @param amount
     *            数值
     * @return 计算后日期字符串
     */
    private static String addInteger(String date, int dateType, int amount){
        String dateString = null;
        DateType dateStyle = getDateStyle(date);
        if(dateStyle != null){
            Date myDate = StringToDate(date, dateStyle);
            myDate = addInteger(myDate, dateType, amount);
            dateString = DateToString(myDate, dateStyle);
        }
        return dateString;
    }
    
    /**
     * 增加日期中某类型的某数值。如增加日期
     * 
     * @param date
     *            日期字符串
     * @param dateType
     *            类型
     * @param amount
     *            数值
     * @return 计算后日期字符串
     */
    private static String addInteger(String date, DateType dateStyle, int dateType, int amount){
        String dateString = null;
        if(dateStyle != null){
            Date myDate = StringToDate(date, dateStyle);
            myDate = addInteger(myDate, dateType, amount);
            dateString = DateToString(myDate, dateStyle);
        }
        return dateString;
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
    private static Date addInteger(Date date, int dateType, int amount){
        Date myDate = null;
        if(date != null){
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(dateType, amount);
            myDate = calendar.getTime();
        }
        return myDate;
    }

    /**
     * 获取精确的日期
     * 
     * @param timestamps
     *            时间long集合
     * @return 日期
     */
    private static Date getAccurateDate(List<Long> timestamps){
        Date date = null;
        long timestamp = 0;
        Map<Long, long[]> map = new HashMap<Long, long[]>();
        List<Long> absoluteValues = new ArrayList<Long>();
        if(timestamps != null && timestamps.size() > 0){
            if(timestamps.size() > 1){
                for(int i = 0; i < timestamps.size(); i++){
                    for(int j = i + 1; j < timestamps.size(); j++){
                        long absoluteValue = Math.abs(timestamps.get(i) - timestamps.get(j));
                        absoluteValues.add(absoluteValue);
                        long[] timestampTmp = { timestamps.get(i), timestamps.get(j) };
                        map.put(absoluteValue, timestampTmp);
                    }
                }
                // 有可能有相等的情况。如2012-11和2012-11-01。时间戳是相等的
                long minAbsoluteValue = -1;
                if(!absoluteValues.isEmpty()){
                    // 如果timestamps的size为2，这是差值只有一个，因此要给默认值
                    minAbsoluteValue = absoluteValues.get(0);
                }
                for(int i = 0; i < absoluteValues.size(); i++){
                    for(int j = i + 1; j < absoluteValues.size(); j++){
                        if(absoluteValues.get(i) > absoluteValues.get(j)){
                            minAbsoluteValue = absoluteValues.get(j);
                        }else{
                            minAbsoluteValue = absoluteValues.get(i);
                        }
                    }
                }
                if(minAbsoluteValue != -1){
                    long[] timestampsLastTmp = map.get(minAbsoluteValue);
                    if(absoluteValues.size() > 1){
                        timestamp = Math.max(timestampsLastTmp[0], timestampsLastTmp[1]);
                    }else if(absoluteValues.size() == 1){
                        // 当timestamps的size为2，需要与当前时间作为参照
                        long dateOne = timestampsLastTmp[0];
                        long dateTwo = timestampsLastTmp[1];
                        if((Math.abs(dateOne - dateTwo)) < 100000000000L){
                            timestamp = Math.max(timestampsLastTmp[0], timestampsLastTmp[1]);
                        }else{
                            long now = new Date().getTime();
                            if(Math.abs(dateOne - now) <= Math.abs(dateTwo - now)){
                                timestamp = dateOne;
                            }else{
                                timestamp = dateTwo;
                            }
                        }
                    }
                }
            }else{
                timestamp = timestamps.get(0);
            }
        }
        if(timestamp != 0){
            date = new Date(timestamp);
        }
        return date;
    }

    /**
     * 判断字符串是否为日期字符串
     * 
     * @param date
     *            日期字符串
     * @return true or false
     */
    public static boolean isDate(String date){
        boolean isDate = false;
        if(date != null){
            if(StringToDate(date) != null){
                isDate = true;
            }
        }
        return isDate;
    }

    /**
     * 获取日期字符串的日期风格。失敗返回null。
     * 
     * @param date
     *            日期字符串
     * @return 日期风格
     */
    public static DateType getDateStyle(String date){
        DateType dateStyle = null;
        Map<Long, DateType> map = new HashMap<Long, DateType>();
        List<Long> timestamps = new ArrayList<Long>();
        for(DateType style : DateType.values()){
            Date dateTmp = StringToDate(date, style.getValue());
            if(dateTmp != null){
                timestamps.add(dateTmp.getTime());
                map.put(dateTmp.getTime(), style);
            }
        }
        dateStyle = map.get(getAccurateDate(timestamps).getTime());
        return dateStyle;
    }

    /**
     * 将日期字符串转化为日期。失败返回null。
     * 
     * @param date
     *            日期字符串
     * @return 日期
     */
    public static Date StringToDate(String date){
        DateType dateStyle = null;
        return StringToDate(date, dateStyle);
    }

    /**
     * 将日期字符串转化为日期。失败返回null。
     * 
     * @param date
     *            日期字符串
     * @param parttern
     *            日期格式
     * @return 日期
     */
    public static Date StringToDate(String date, String parttern){
        Date myDate = null;
        if(date != null){
            try{
                myDate = getDateFormat(parttern).parse(date);
            }catch(Exception e){
            }
        }
        return myDate;
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
    public static Date stringToDate(String date, String pattern, String timeZone){
        Date myDate = null;
        if(date != null){
            try{
                SimpleDateFormat format = getDateFormat(pattern);
                format.setTimeZone(TimeZone.getTimeZone(timeZone));
                myDate = format.parse(date);
            }catch(Exception e){
                LOGGER.error("转换日期失败, date:{}",date, e);
                return null;
            }
        }
        return myDate;
    }

    public static String utcToGmt(String strDate){
        Date date = DateUtil.stringToDate(strDate, DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(),"UTC");
        return DateToString(date, DateStyle.YYYY_MM_DD_HH_MM_SS);
    }

    public static Date utcToGmtDate(String strDate){
        return DateUtil.stringToDate(strDate, DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(),"UTC");
    }

    public static Date localToUTC(String localTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date localDate= null;
        try {
            localDate = sdf.parse(localTime);
        } catch (ParseException e) {
            LOGGER.error("转换日期失败, date:{}",localTime, e);
            return null;
        }
        long localTimeInMillis=localDate.getTime();
        Calendar calendar= Calendar.getInstance();
        calendar.setTimeInMillis(localTimeInMillis);
        // 取得时间偏移量
        int zoneOffset = calendar.get(Calendar.ZONE_OFFSET);
        // 取得夏令时差
        int dstOffset = calendar.get(Calendar.DST_OFFSET);
        // 从本地时间里扣除这些差量，即可以取得UTC时间
        calendar.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        Date utcDate=new Date(calendar.getTimeInMillis());
        return utcDate;
    }

    public static int getTimeInt(Date date){
        return (int) (date.getTime()/MILLISECONDS_OF_SECOND);
    }

    /**
     * 将日期字符串转化为日期。失败返回null。
     * 
     * @param date
     *            日期字符串
     * @param dateStyle
     *            日期风格
     * @return 日期
     */
    public static Date StringToDate(String date, DateType dateStyle){
        Date myDate = null;
        if(dateStyle == null){
            List<Long> timestamps = new ArrayList<Long>();
            for(DateType style : DateType.values()){
                Date dateTmp = StringToDate(date, style.getValue());
                if(dateTmp != null){
                    timestamps.add(dateTmp.getTime());
                }
            }
            myDate = getAccurateDate(timestamps);
        }else{
            myDate = StringToDate(date, dateStyle.getValue());
        }
        return myDate;
    }

    /**
     * 将日期转化为日期字符串。失败返回null。
     * 
     * @param date
     *            日期
     * @param parttern
     *            日期格式
     * @return 日期字符串
     */
    public static String DateToString(Date date, String parttern){
        String dateString = null;
        if(date != null){
            try{
                dateString = getDateFormat(parttern).format(date);
            }catch(Exception e){
            }
        }
        return dateString;
    }

    /**
     * 将日期转化为日期字符串。失败返回null。
     * 
     * @param date
     *            日期
     * @param dateStyle
     *            日期风格
     * @return 日期字符串
     */
    public static String DateToString(Date date, DateType dateStyle){
        String dateString = null;
        if(dateStyle != null){
            dateString = DateToString(date, dateStyle.getValue());
        }
        return dateString;
    }

    /**
     * 将日期字符串转化为另一日期字符串。失败返回null。
     * 
     * @param date
     *            旧日期字符串
     * @param parttern
     *            新日期格式
     * @return 新日期字符串
     */
    public static String StringToString(String date, String parttern){
        return StringToString(date, null, parttern);
    }

    /**
     * 将日期字符串转化为另一日期字符串。失败返回null。
     * 
     * @param date
     *            旧日期字符串
     * @param dateType
     *            新日期风格
     * @return 新日期字符串
     */
    public static String StringToString(String date, DateType dateType){
        return StringToString(date, null, dateType);
    }

    /**
     * 将日期字符串转化为另一日期字符串。失败返回null。
     * 
     * @param date
     *            旧日期字符串
     * @param olddParttern
     *            旧日期格式
     * @param newParttern
     *            新日期格式
     * @return 新日期字符串
     */
    public static String StringToString(String date, String olddParttern, String newParttern){
        String dateString = null;
        if(olddParttern == null){
            DateType style = getDateStyle(date);
            if(style != null){
                Date myDate = StringToDate(date, style.getValue());
                dateString = DateToString(myDate, newParttern);
            }
        }else{
            Date myDate = StringToDate(date, olddParttern);
            dateString = DateToString(myDate, newParttern);
        }
        return dateString;
    }

    /**
     * 将日期字符串转化为另一日期字符串。失败返回null。
     * 
     * @param date
     *            旧日期字符串
     * @param olddDteStyle
     *            旧日期风格
     * @param newDateStyle
     *            新日期风格
     * @return 新日期字符串
     */
    public static String StringToString(String date, DateType olddDteStyle, DateType newDateStyle){
        String dateString = null;
        if(olddDteStyle == null){
            DateType style = getDateStyle(date);
            dateString = StringToString(date, style.getValue(), newDateStyle.getValue());
        }else{
            dateString = StringToString(date, olddDteStyle.getValue(), newDateStyle.getValue());
        }
        return dateString;
    }

    /**
     * 增加日期的年份。失败返回null。
     * 
     * @param date
     *            日期
     * @param yearAmount
     *            增加数量。可为负数
     * @return 增加年份后的日期字符串
     */
    public static String addYear(String date, int yearAmount){
        return addInteger(date, Calendar.YEAR, yearAmount);
    }

    /**
     * 增加日期的年份。失败返回null。
     * 
     * @param date
     *            日期
     * @param yearAmount
     *            增加数量。可为负数
     * @return 增加年份后的日期
     */
    public static Date addYear(Date date, int yearAmount){
        return addInteger(date, Calendar.YEAR, yearAmount);
    }

    /**
     * 增加日期的月份。失败返回null。
     * 
     * @param date
     *            日期
     * @param yearAmount
     *            增加数量。可为负数
     * @return 增加月份后的日期字符串
     */
    public static String addMonth(String date, int yearAmount){
        return addInteger(date, Calendar.MONTH, yearAmount);
    }

    /**
     * 增加日期的月份。失败返回null。
     * 
     * @param date
     *            日期
     * @param yearAmount
     *            增加数量。可为负数
     * @return 增加月份后的日期
     */
    public static Date addMonth(Date date, int yearAmount){
        return addInteger(date, Calendar.MONTH, yearAmount);
    }

    /**
     * 增加日期的天数。失败返回null。
     * 
     * @param date
     *            日期字符串
     * @param dayAmount
     *            增加数量。可为负数
     * @return 增加天数后的日期字符串
     */
    public static String addDay(String date, int dayAmount){
        return addInteger(date, Calendar.DATE, dayAmount);
    }
    
    /**
     * 增加日期的天数。失败返回null。
     * 
     * @param date
     *            日期字符串
     * @param dayAmount
     *            增加数量。可为负数
     * @param style
     * 			  日期样式
     * @return 增加天数后的日期字符串
     */
    public static String addDay(String date, int dayAmount, DateType style){
        return addInteger(date, style, Calendar.DATE, dayAmount);
    }

    /**
     * 增加日期的天数。失败返回null。
     * 
     * @param date
     *            日期
     * @param dayAmount
     *            增加数量。可为负数
     * @return 增加天数后的日期
     */
    public static Date addDay(Date date, int dayAmount){
        return addInteger(date, Calendar.DATE, dayAmount);
    }

    /**
     * 增加日期的小时。失败返回null。
     * 
     * @param date
     *            日期字符串
     * @param hourAmount
     *            增加数量。可为负数
     * @return 增加小时后的日期字符串
     */
    public static String addHour(String date, int hourAmount){
        return addInteger(date, Calendar.HOUR_OF_DAY, hourAmount);
    }

    /**
     * 增加日期的小时。失败返回null。
     * 
     * @param date
     *            日期
     * @param hourAmount
     *            增加数量。可为负数
     * @return 增加小时后的日期
     */
    public static Date addHour(Date date, int hourAmount){
        return addInteger(date, Calendar.HOUR_OF_DAY, hourAmount);
    }

    /**
     * 增加日期的分钟。失败返回null。
     * 
     * @param date
     *            日期字符串
     * @param hourAmount
     *            增加数量。可为负数
     * @return 增加分钟后的日期字符串
     */
    public static String addMinute(String date, int hourAmount){
        return addInteger(date, Calendar.MINUTE, hourAmount);
    }

    /**
     * 增加日期的分钟。失败返回null。
     * 
     * @param date
     *            日期
     * @param minuteAmount
     *            增加数量。可为负数
     * @return 增加分钟后的日期
     */
    public static Date addMinute(Date date, int minuteAmount){
        return addInteger(date, Calendar.MINUTE, minuteAmount);
    }

    /**
     * 增加日期的秒钟。失败返回null。
     * 
     * @param date
     *            日期字符串
     * @param hourAmount
     *            增加数量。可为负数
     * @return 增加秒钟后的日期字符串
     */
    public static String addSecond(String date, int hourAmount){
        return addInteger(date, Calendar.SECOND, hourAmount);
    }

    /**
     * 增加日期的秒钟。失败返回null。
     * 
     * @param date
     *            日期
     * @param hourAmount
     *            增加数量。可为负数
     * @return 增加秒钟后的日期
     */
    public static Date addSecond(Date date, int hourAmount){
        return addInteger(date, Calendar.SECOND, hourAmount);
    }

    public static Date addTime(Date date, String timeUnit, int amount){
        String lowerCaseTimeUnit = timeUnit.toLowerCase();
        switch (lowerCaseTimeUnit){
            case TIME_UNIT_SECOND:
                return addInteger(date, Calendar.SECOND, amount);
            case TIME_UNIT_MINUTE:
                return addInteger(date, Calendar.MINUTE, amount);
            case TIME_UNIT_HOUR:
                return addInteger(date, Calendar.HOUR, amount);
            case TIME_UNIT_DAY:
                return addInteger(date, Calendar.DAY_OF_YEAR, amount);
            default:
                return date;
        }
    }

    /**
     * 获取日期的年份。失败返回0。
     * 
     * @param date
     *            日期字符串
     * @return 年份
     */
    public static int getYear(String date){
        return getYear(StringToDate(date));
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
     *            日期字符串
     * @return 月份
     */
    public static int getMonth(String date){
        return getMonth(StringToDate(date));
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
     * 获取日期的天数。失败返回0。
     * 
     * @param date
     *            日期字符串
     * @return 天
     */
    public static int getDay(String date){
        return getDay(StringToDate(date));
    }

    /**
     * 获取日期的天数。失败返回0。
     * 
     * @param date
     *            日期
     * @return 天
     */
    public static int getDay(Date date){
        return getInteger(date, Calendar.DATE);
    }

    /**
     * 获取日期的小时。失败返回0。
     * 
     * @param date
     *            日期字符串
     * @return 小时
     */
    public static int getHour(String date){
        return getHour(StringToDate(date));
    }

    /**
     * 获取日期的小时。失败返回0。
     * 
     * @param date
     *            日期
     * @return 小时
     */
    public static int getHour(Date date){
        return getInteger(date, Calendar.HOUR_OF_DAY);
    }

    /**
     * 获取日期的分钟。失败返回0。
     * 
     * @param date
     *            日期字符串
     * @return 分钟
     */
    public static int getMinute(String date){
        return getMinute(StringToDate(date));
    }

    /**
     * 获取日期的分钟。失败返回0。
     * 
     * @param date
     *            日期
     * @return 分钟
     */
    public static int getMinute(Date date){
        return getInteger(date, Calendar.MINUTE);
    }

    /**
     * 获取日期的秒钟。失败返回0。
     * 
     * @param date
     *            日期字符串
     * @return 秒钟
     */
    public static int getSecond(String date){
        return getSecond(StringToDate(date));
    }

    /**
     * 获取日期的秒钟。失败返回0。
     * 
     * @param date
     *            日期
     * @return 秒钟
     */
    public static int getSecond(Date date){
        return getInteger(date, Calendar.SECOND);
    }

    /**
     * 获取日期 。默认yyyy-MM-dd格式。失败返回null。
     * 
     * @param date
     *            日期字符串
     * @return 日期
     */
    public static String getDate(String date){
        return StringToString(date, DateType.YYYY_MM_DD);
    }

    /**
     * 获取日期。默认yyyy-MM-dd格式。失败返回null。
     * 
     * @param date
     *            日期
     * @return 日期
     */
    public static String getDate(Date date){
        return DateToString(date, DateType.YYYY_MM_DD);
    }

    /**
     * 获取日期的时间。默认HH:mm:ss格式。失败返回null。
     * 
     * @param date
     *            日期字符串
     * @return 时间
     */
    public static String getTime(String date){
        return StringToString(date, DateType.HH_MM_SS);
    }

    /**
     * 获取日期的时间。默认HH:mm:ss格式。失败返回null。
     * 
     * @param date
     *            日期
     * @return 时间
     */
    public static String getTime(Date date){
        return DateToString(date, DateType.HH_MM_SS);
    }


    /**
     * 获取两个日期相差的天数
     * 
     * @param date
     *            日期字符串
     * @param otherDate
     *            另一个日期字符串
     * @return 相差天数
     */
    public static int getIntervalDays(String date, String otherDate){
        return getIntervalDays(StringToDate(date), StringToDate(otherDate));
    }

    /**
     * @param date
     *            日期
     * @param otherDate
     *            另一个日期
     * @return 相差天数
     */
    public static int getIntervalDays(Date date, Date otherDate){
        Date formattedDate = DateUtil.StringToDate(DateUtil.getDate(date));
        long time = Math.abs(formattedDate.getTime() - otherDate.getTime());
        return (int) time / MILLISECONDS_OF_DAY;
    }

    /**
     * 获取两个日期相差的秒数
     *
     * @param date 日期的秒数
     * @param otherDate 另一个日期的秒数
     * @return 相差秒数
     */
    public static int getIntervalSeconds(long date, long otherDate){
        long time = Math.abs(date - otherDate);
        return (int) time / MILLISECONDS_OF_SECOND;
    }

    public static String getWeekFromDate(Date date){
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int week = c.get(Calendar.DAY_OF_WEEK);
        switch (week) {
            case Calendar.SUNDAY:
                return "星期日";
            case Calendar.MONDAY:
                return "星期一";
            case Calendar.TUESDAY:
                return "星期二";
            case Calendar.WEDNESDAY:
                return "星期三";
            case Calendar.THURSDAY:
                return "星期四";
            case Calendar.FRIDAY:
                return "星期五";
            case Calendar.SATURDAY:
                return "星期六";
            default:
                return "";
        }
    }

    /**
     * 获取指定天数之前的时间（相对于当前天数）
     * 
     * @Title: getLastDays
     * @param currentDate
     *            当前时间
     * @param dayNo
     *            当前时间的几天之前
     * @return
     * @date 2014-3-26 下午11:23:57
     * @author Lawrence
     */
    public static Date getLastTimeForDays(Date currentDate, Integer dayNo){
        if(null == currentDate || null == dayNo){
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(currentDate);
        c.add(Calendar.DATE, -dayNo);
        return c.getTime();
    }

    /**
     * 获取指定月数之前的时间（相对于当前天数）
     * 
     * @Title: getLastDays
     * @param currentDate
     *            当前时间
     * @param monthNo
     *            当前时间的几月之前
     * @return
     * @date 2014-3-26 下午11:23:57
     * @author Lawrence
     */
    public static Date getLastTimeForMonths(Date currentDate, Integer monthNo){
        if(null == currentDate || null == monthNo){
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(currentDate);
        c.add(Calendar.MONTH, -monthNo);
        return c.getTime();
    }

    /**
     * 获取指定月数之前的时间（相对于当前天数）
     * 
     * @Title: getLastDays
     * @param currentDate
     *            当前时间
     * @param yearNo
     *            当前时间的几年之前
     * @return
     * @date 2014-3-26 下午11:23:57
     * @author Lawrence
     */
    public static Date getLastTimeForYears(Date currentDate, Integer yearNo){
        Calendar c = Calendar.getInstance();
        c.setTime(currentDate);
        c.add(Calendar.YEAR, -yearNo);
        return c.getTime();
    }


    /**
     * 获取当天00:00:00
     * @Title: getZeroPoint 
     * @Description: (这里用一句话描述这个方法的作用) 
     * @param currentDate
     * @return
     * @date 2014年10月10日 上午11:09:35  
     * @author Administrator
     */
    public static Date getZeroPoint(Date currentDate){
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);
        cal.set(Calendar.HOUR_OF_DAY, 0); // 把当前时间小时变成０
        cal.set(Calendar.MINUTE, 0); // 把当前时间分钟变成０
        cal.set(Calendar.SECOND, 0); // 把当前时间秒数变成０
        cal.set(Calendar.MILLISECOND, 0); // 把当前时间毫秒变成０
        return cal.getTime();
    }
    
	 /**
     * 把毫秒转化成日期
     * @param millSec(毫秒数)
     * @return
     */
    public static Date LongToDate(Long millSec){
    	return LongToDate(millSec,"yyyy-MM-dd HH:mm:ss");
    }
    
	 /**
     * 把毫秒转化成日期
     * @param parttern(日期格式，例如：MM/ dd/yyyy HH:mm:ss)
     * @param millSec(毫秒数)
     * @return
     */
    public static Date LongToDate(Long millSec,String parttern){
     return new Date(millSec);
    }

    public static String getDuration(Long millSec){
        long days = millSec / MILLISECONDS_OF_DAY;
        long hours = (millSec % MILLISECONDS_OF_DAY) / MILLISECONDS_OF_HOUR;
        long minutes = (millSec % MILLISECONDS_OF_HOUR) / MILLISECONDS_OF_MINUTE;
        long seconds = (millSec % MILLISECONDS_OF_MINUTE) / MILLISECONDS_OF_SECOND;
        return (days>0 ? days + "d":"") + (hours>0?hours + "h":"") + (minutes>0? minutes + "m":"")
                + seconds + "s";
    }

    public static String getDuration(String millSecStr){
        return getDuration(Long.valueOf(millSecStr));
    }

    public static Integer getSinceSeconds(Integer num, String timeUnit){
        if(num == null || StringUtils.isBlank(timeUnit)){
            return null;
        }
        Integer sinceSeconds = 0;
        String unit = timeUnit.toLowerCase();
        switch (unit){
            case "m":
                sinceSeconds = SECONDS_OF_MINUTE * num;
                break;
            case "h":
                sinceSeconds = SECONDS_OF_MINUTE * MINUTES_OF_HOUR * num;
                break;
            case "d":
                sinceSeconds = SECONDS_OF_MINUTE * MINUTES_OF_HOUR * HOURS_OF_DAY * num;
                break;
            default:
                sinceSeconds = 0;
        }
        return sinceSeconds;
    }

    /**
     * 根据时区获取带时区的日期格式
     * @param timeZone
     * @return
     */
    public static String getTimezoneFormatStyle(TimeZone timeZone) {
        int hour = timeZone.getRawOffset() / MILLISECONDS_OF_HOUR;
        int minute = Math.abs((timeZone.getRawOffset() - hour * MILLISECONDS_OF_HOUR) / MILLISECONDS_OF_MINUTE);
        String hourMinute = hour + "";
        if (hour > -NUM_TEN && hour < 0) {
            hourMinute = "-0" + Math.abs(hour);
        } else if (hour >= 0 && hour < NUM_TEN) {
            hourMinute = "+0" + hourMinute;
        } else if (hour > NUM_NINE) {
            hourMinute = "+" + hourMinute;
        }
        if (minute == 0) {
            hourMinute += ":00";
        } else {
            hourMinute += ":" + minute;
        }
        String style = "yyyy-MM-dd'T'HH:mm:ss'" + hourMinute + "'";
        return style;
    }

    public static String getLaterTime(String x, String y) throws Exception {
        if(StringUtils.isBlank(x)){
            return y;
        }else if(StringUtils.isBlank(y)){
            return x;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        int b = Long.valueOf(sdf.parse(x).getTime()).compareTo(Long.valueOf(sdf.parse(y).getTime()));
        if (b == -1) {
            return y;
        }else{
            return x;
        }
    }
	
	
    /**
     * 函数功能描述:UTC时间转本地时间格式
     * @param utcTime UTC时间
     * @param utcTimePatten UTC时间格式
     * @param localTimePatten   本地时间格式
     * @return 本地时间格式的时间
     * eg:utc2Local("2017-06-14 09:37:50.788+08:00", "yyyy-MM-dd HH:mm:ss.SSSSSS", "yyyy-MM-dd HH:mm:ss.SSS")
     */
    public static String utc2Local(String utcTime, String utcTimePatten, String localTimePatten) {
        SimpleDateFormat utcFormater = new SimpleDateFormat(utcTimePatten);
        //时区定义并进行时间获取
        utcFormater.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date gpsUTCDate;
        try {
            gpsUTCDate = utcFormater.parse(utcTime);
        } catch (ParseException e) {
            return utcTime;
        }
        SimpleDateFormat localFormater = new SimpleDateFormat(localTimePatten);
        localFormater.setTimeZone(TimeZone.getDefault());
        String localTime = localFormater.format(gpsUTCDate.getTime());
        return localTime;
    }

    /**
     * UTC时间转本地时间格式
     * @param date
     * @param localTimePatten
     * @return
     */
    public static String utc2Local(Date date, String localTimePatten) {
        SimpleDateFormat localFormater = new SimpleDateFormat(localTimePatten);
        localFormater.setTimeZone(TimeZone.getDefault());
        String localTime = localFormater.format(date.getTime());
        return localTime;
    }

    /**
     * 函数功能描述:UTC时间转本地时间格式
     * @param utcTime UTC时间
     * @param utcTimePatten UTC时间格式
     * @param localTimePatten   本地时间格式
     * @return 本地时间格式的时间
     * eg:utc2Local("2017-06-14 09:37:50.788+08:00", "yyyy-MM-dd HH:mm:ss.SSSSSS", "yyyy-MM-dd HH:mm:ss.SSS")
     */
    public static Date utc2LocalDate(String utcTime, String utcTimePatten, String localTimePatten) {
        SimpleDateFormat utcFormater = new SimpleDateFormat(utcTimePatten);
        //时区定义并进行时间获取
        utcFormater.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date gpsUTCDate = new Date();
        try {
            gpsUTCDate = utcFormater.parse(utcTime);
        } catch (ParseException e) {
            e.printStackTrace();
            return gpsUTCDate;
        }
        SimpleDateFormat localFormater = new SimpleDateFormat(localTimePatten);
        localFormater.setTimeZone(TimeZone.getDefault());
        return gpsUTCDate;
    }
    /**
     * 函数功能描述:UTC时间转本地时间格式
     * @param localTime UTC时间
     * @param utcTimePatten UTC时间格式
     * @param localTimePatten   本地时间格式
     * @return 本地时间格式的时间
     * eg:utc2Local("2017-06-14 09:37:50", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss'Z'", )
     */
    public static String local2Utc(String localTime, String localTimePatten, String utcTimePatten) {
        SimpleDateFormat localFormater = new SimpleDateFormat(localTimePatten);
        //时区定义并进行时间获取
        localFormater.setTimeZone(TimeZone.getDefault());
        Date gpsLocalDate;
        try {
            gpsLocalDate = localFormater.parse(localTime);
        } catch (ParseException e) {
            e.printStackTrace();
            return localTime;
        }
        SimpleDateFormat utcFormater = new SimpleDateFormat(utcTimePatten);
        utcFormater.setTimeZone(TimeZone.getTimeZone("UTC"));
        String utcTime = utcFormater.format(gpsLocalDate.getTime());
        return utcTime;
    }

    /**
     * 格式化Date时间为指定时区
     *
     * @param time       Long类型时间戳
     * @param timeFromat String类型格式
     * @return 格式化后的字符串
     */
    public static String formatDateByZone(Long time, String timeFromat,TimeZone timeZone) {
        DateFormat dateFormat = new SimpleDateFormat(timeFromat);
        if(null!=timeZone){
            dateFormat.setTimeZone(timeZone);
        }
        return dateFormat.format(time);
    }

    /**
     * LocalDateTime转成String
     *
     * @param time LocalDateTime类型时间
     * @param format 格式
     * @return
     */
    public static String localDateTimeToString(LocalDateTime time, String format) {
        DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern(format);
        return dtf2.format(time);
    }

    /**
     * 获取近24小时的所有小时
     * @param current
     * @return
     */
    public static List<String> calcHour(Date current) {
        List<String> hours = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Date ago = DateUtil.addHour(current, -(23-i));
            String agoStr = DateUtils.DateToString(ago, "yyyy-MM-dd HH:00:00");
            hours.add(agoStr);
        }
        return hours;
    }

    /**
     *
     * @param date
     * @param count
     * @return
     */
    public static String addHourAndGetNormal(String date, int count) {
        Date original = DateUtils.parseDate(date, DateType.YYYY_MM_DD_HH_MM_SS.getValue());
        Date real = DateUtil.addHour(original, count);
        String realDate = DateUtils.DateToString(real, DateType.YYYY_MM_DD_HH_MM_SS.getValue());
        return realDate;
    }

    public static String parseUTCDate(String datetime) {
        Date date = DateUtils.parseDate(datetime, DateStyle.YYYY_MM_DD_HH_MM_SS);
        date = DateUtil.addHour(date, -8);
        return DateUtils.dateToString(date, DateStyle.YYYY_MM_DD_HH_MM_SS);
    }

}
