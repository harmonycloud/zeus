package com.harmonycloud.zeus.util;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author liyinlong
 * @since 2022/10/18 4:58 下午
 */
public class MysqlUtil {

    /**
     * mysql数据库中保存字符集和排序规则使用一个字段保存：即：字符集_排序规则，
     * 如果需要字符集，则需要使用_分割提取
     */
    public static String extractCharset(String collation) {
        if (StringUtils.isEmpty(collation) || !collation.contains("_")) {
            return "";
        }
        return collation.substring(0, collation.indexOf("_"));
    }

    /**
     * mysql数据库中数据表的列是否允许为null是以no或yes存储的，
     * 为了方便表达，平台使用boolean类型（true或false）来表示是否允许为null
     */
    public static boolean convertColumnNullable(String nullAble) {
        return !nullAble.equalsIgnoreCase("no");
    }

    /**
     * mysql中使用COLUMN_KEY来存储某一列是否是主键，当值为PRI时表示该列是主键
     */
    public static boolean convertColumnPrimary(String columnKey) {
        return columnKey.contains("PRI");
    }

    /**
     * mysql使用extra字段来存储某一列是否允许自增，当extra中含有auto_increment时，
     * 表示该列允许自增
     */
    public static boolean convertAutoIncrement(String extra) {
        return extra.contains("auto_increment");
    }

    /**
     * mysql中不直接存储某一列的数据长度，而是存储数据类型加长度，
     * 例如 长度为128的varchar类型的数据存储为varchar(128)，因此，
     * 如果要获取某一列的数据长度，则需要根据数据类型提取
     * dataType：数据类型 例如：varchar(128)
     */
    public static Integer convertColumnDataSize(String dataType) {
        if (StringUtils.isEmpty(dataType)) {
            return null;
        }
        String reg3 = "(?<=\\()[\\s\\S]*(?=\\))";
        Pattern p3 = Pattern.compile(reg3);
        Matcher m3 = p3.matcher(dataType);
        if (m3.find()) {
            String size = m3.group().split(",")[0];
            return Integer.parseInt(size);
        }
        return null;
    }

    /**
     * mysql中使用Y或N来表示是否拥有某个权限，如果拥有权限则值为Y，
     * 反之为N,平台使用bool值来表示，这里将权限值转为bool值
     */
    public static boolean convertGrantPriv(String priv) {
        return priv.equalsIgnoreCase("Y");
    }

    /**
     * mysql中使用YES或NO来表示是否可以传递权限，如果可以传递则为YES，反之为NO，
     * 平台使用bool值来表示，这里将权限值转为bool值
     */
    public static boolean convertGrantAble(String grantAble) {
        return grantAble.equalsIgnoreCase("YES");
    }

    /**
     * 将空串转为null
     * @param value
     * @return
     */
    public static String convertEmptyToNull(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return value;
    }

    public static void main(String[] args) {

        convertColumnDataSize("varchar(125)");
    }

}
