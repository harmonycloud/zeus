package com.middleware.zeus.util;

/**
 * @author liyinlong
 * @since 2023/4/28 11:11 上午
 */
public class VersionUtil {

    public static int countDots(String str) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '.') {
                count++;
            }
        }
        return count;
    }

}
