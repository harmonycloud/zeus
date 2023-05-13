package com.middleware.zeus.util.encrypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.Random;

/**
 * @author dengyulong
 * @date 2021/04/02
 */
public class PasswordUtils {

    private static final Logger logger = LoggerFactory.getLogger(PasswordUtils.class);

    private static final char[] CHARS_WITH_SPECIAL_CHARACTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890~!@#$%^&*.?".toCharArray();
    private static final char[] CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();

    /**
     * 生成8位不包含特殊字符的密码
     */
    public static String generateCommonPassword() {
        return generateRandomPassword(8, false);
    }

    /**
     * 生成不包含特殊字符的密码
     */
    public static String generateCommonPassword(int len) {
        return generateRandomPassword(len, false);
    }

    /**
     * 生成8位包含特殊字符的密码
     */
    public static String generateSpecialPassword() {
        return generateRandomPassword(8, true);
    }

    /**
     * 生成包含特殊字符的密码
     */
    public static String generateSpecialPassword(int len) {
        return generateRandomPassword(len, true);
    }

    /**
     * 生成密码并验证
     */
    private static String generateRandomPassword(int len, boolean withSpecialCharacters) {
        String result = generateRandomPasswordWithoutValidation(len, withSpecialCharacters);
        if (result.matches(".*[a-z]{1,}.*") && result.matches(".*[A-Z]{1,}.*") && result.matches(".*[0-9]{1,}.*")
            && (!withSpecialCharacters || result.matches(".*[~!@#$%^&*\\.?]{1,}.*"))) {
            return result;
        }
        return generateRandomPassword(len, withSpecialCharacters);
    }

    /**
     * 生成密码（不含验证）
     *
     * @param length                密码长度
     * @param withSpecialCharacters 是否包含特殊字符
     * @return
     */
    private static String generateRandomPasswordWithoutValidation(int length, boolean withSpecialCharacters) {
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        char[] charArray = withSpecialCharacters ? CHARS_WITH_SPECIAL_CHARACTERS : CHARS;
        for (int x = 0; x < length; ++x) {
            sb.append(charArray[r.nextInt(charArray.length)]);
        }
        return sb.toString();
    }

    /**
     * MD5加密,结果为大写
     */
    public static String md5(String string) {
        if (string == null || "".equals(string)) {
            return null;
        }
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        try {
            byte[] btInput = string.getBytes();
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char[] str = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            logger.warn("MD5加密失败：" + string, e);
            return null;
        }
    }
    
}
