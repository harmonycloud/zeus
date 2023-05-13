package com.middleware.zeus.util;

/**
 * @author liyinlong
 * @since 2022/10/28 5:20 下午
 */
public class DateTimeUtil {

    public static Integer convertExpirationToSeconds(String expiration) {
        String[] ary = expiration.split("[a-z]");
        int s = 0;
        switch (ary.length) {
            case 3:
                s = 3600 * Integer.parseInt(ary[0]) + 60 * Integer.parseInt(ary[1]) + Integer.parseInt(ary[2]);
                break;
            case 2:
                s = 60 * Integer.parseInt(ary[0]) + Integer.parseInt(ary[1]);
                break;
            case 1:
                s = Integer.parseInt(ary[0]);
                break;
            default:
        }
        return s;
    }

    public static void main(String[] args) {

        System.out.println(convertExpirationToSeconds("138h53m17s"));
    }

}
