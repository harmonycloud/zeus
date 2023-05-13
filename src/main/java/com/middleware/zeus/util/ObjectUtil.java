package com.middleware.zeus.util;

import org.apache.commons.lang3.StringUtils;

/**
 * @author liyinlong
 * @since 2022/11/8 10:31 上午
 */
public class ObjectUtil {

    /**
     * 判断两个字符串是否相等，若a为null，b为"",也认为这两个字符串是相等的
     * @param a
     * @param b
     * @return
     */
    public static boolean equals(String a, String b) {
        if (StringUtils.isEmpty(a) && StringUtils.isEmpty(b)) {
            return true;
        }
        return StringUtils.equals(a, b);
    }


    public static void main(String[] args) {
        String a = "a";

        String b = "";

        System.out.println(equals(a,b));
    }

}
