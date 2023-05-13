package com.middleware.zeus.util.encrypt;

import java.util.Base64;

/**
 * @author dengyulong
 * @date 2021/04/26
 */
public class Base64Utils {

    /**
     * <p>
     * BASE64字符串解码为二进制数据
     * </p>
     *
     * @param base64
     * @return
     */
    public static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64.getBytes());
    }

    /**
     * <p>
     * 二进制数据编码为BASE64字符串
     * </p>
     *
     * @param bytes
     * @return
     */
    public static String encode(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes));
    }

}
