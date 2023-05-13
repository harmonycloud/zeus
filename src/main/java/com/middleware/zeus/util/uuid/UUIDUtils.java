package com.middleware.zeus.util.uuid;

import java.util.UUID;

/**
 * @author chwetion
 * @since 2020/11/27 10:59 上午
 */
public class UUIDUtils {

    public static final int UUID_LENGTH_16 = 16;
    public static final int UUID_LENGTH_8 = 8;

    /**
     * 生成随机字符串UUID（长度32）
     *
     * @return
     */
    public static String getUUID() {
        UUID uuid = UUID.randomUUID();
        String str = uuid.toString();
        return str.replace("-", "");
    }

    /**
     * 生成16位随机字符串UUID
     *
     * @return
     */
    public static String get16UUID() {
        return getUUID().substring(0, UUID_LENGTH_16);
    }

    /**
     * 生成8位随机字符串UUID
     *
     * @return
     */
    public static String get8UUID() {
        return getUUID().substring(0, UUID_LENGTH_8);
    }

}
