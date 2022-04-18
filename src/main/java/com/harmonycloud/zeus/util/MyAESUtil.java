package com.harmonycloud.zeus.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES加密解密
 */
public class MyAESUtil {

    private static final String default_key = "Hc@Cloud01";
    static final Base64.Decoder decoder = Base64.getDecoder();
    static final Base64.Encoder encoder = Base64.getEncoder();
    static final String charset = "utf-8";
    static final String AES = "AES";

    /**
     * 先AES加密，再Base64加密
     *
     * @param content
     * @return
     */
    public static String encodeBase64(String content) {
        String encode = encode(content);//AES加密
        if (encode == null) return null;//加密失败返回空
        try {
            String s = encoder.encodeToString(encode.getBytes(charset));//Base64加密
            return s;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 先Base64解密，再AES解密
     *
     * @param content
     * @return
     */
    public static String decodeBase64(String content) {
        try {
            String s = new String(decoder.decode(content), charset);//Base64解密
            String decode = decode(s);//AES解密
            return decode;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;//解密失败返回空
    }

    /**
     * AES加密
     *
     * @param content
     * @return
     */
    private static String encode(String content) {
        return encode(default_key, content);
    }

    /**
     * AES解密
     *
     * @param content
     * @return
     */
    private static String decode(String content) {
        return decode(default_key, content);
    }

    /*
     * AES加密
     * 1.构造密钥生成器
     * 2.根据ecnodeRules规则初始化密钥生成器
     * 3.产生密钥
     * 4.创建和初始化密码器
     * 5.内容加密
     * 6.返回字符串
     */
    private static String encode(String encodeRules, String content) {
        try {
            //1.构造密钥生成器，指定为AES算法,不区分大小写
            KeyGenerator keygen = KeyGenerator.getInstance(AES);
            //2.根据ecnodeRules规则初始化密钥生成器
            //生成一个128位的随机源,根据传入的字节数组
//            keygen.init(128, new SecureRandom(encodeRules.getBytes()));
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(encodeRules.getBytes());
            keygen.init(128, random);
            //3.产生原始对称密钥
            SecretKey original_key = keygen.generateKey();
            //4.获得原始对称密钥的字节数组
            byte[] raw = original_key.getEncoded();
            //5.根据字节数组生成AES密钥
            SecretKey key = new SecretKeySpec(raw, AES);
            //6.根据指定算法AES自成密码器
            Cipher cipher = Cipher.getInstance(AES);
            //7.初始化密码器，第一个参数为加密(Encrypt_mode)或者解密解密(Decrypt_mode)操作，第二个参数为使用的KEY
            cipher.init(Cipher.ENCRYPT_MODE, key);
            //8.获取加密内容的字节数组(这里要设置为utf-8)不然内容中如果有中文和英文混合中文就会解密为乱码
            byte[] byte_encode = content.getBytes(charset);
            //9.根据密码器的初始化方式--加密：将数据加密
            byte[] byte_AES = cipher.doFinal(byte_encode);
            //10.将加密后的数据转换为字符串
            //这里用Base64Encoder中会找不到包
            //解决办法：
            //在项目的Build path中先移除JRE System Library，再添加库JRE System Library，重新编译后就一切正常了。
            String AES_encode = new String(Base64.getEncoder().encode(byte_AES));
            //11.将字符串返回
            return AES_encode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;//加密失败返回空
    }

    /*
     * AES解密
     * 解密过程：
     * 1.同加密1-4步
     * 2.将加密后的字符串反纺成byte[]数组
     * 3.将加密内容解密
     */
    private static String decode(String encodeRules, String content) {
        try {
            //1.构造密钥生成器，指定为AES算法,不区分大小写
            KeyGenerator keygen = KeyGenerator.getInstance(AES);
            //2.根据ecnodeRules规则初始化密钥生成器
            //生成一个128位的随机源,根据传入的字节数组
//            keygen.init(128, new SecureRandom(encodeRules.getBytes()));
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(encodeRules.getBytes());
            keygen.init(128, random);
            //3.产生原始对称密钥
            SecretKey original_key = keygen.generateKey();
            //4.获得原始对称密钥的字节数组
            byte[] raw = original_key.getEncoded();
            //5.根据字节数组生成AES密钥
            SecretKey key = new SecretKeySpec(raw, AES);
            //6.根据指定算法AES自成密码器
            Cipher cipher = Cipher.getInstance(AES);
            //7.初始化密码器，第一个参数为加密(Encrypt_mode)或者解密(Decrypt_mode)操作，第二个参数为使用的KEY
            cipher.init(Cipher.DECRYPT_MODE, key);
            //8.将加密并编码后的内容解码成字节数组
            byte[] byte_content = Base64.getDecoder().decode(content);
            /*
             * 解密
             */
            byte[] byte_decode = cipher.doFinal(byte_content);
            String AES_decode = new String(byte_decode, charset);
            return AES_decode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;//解密失败返回空
    }

    public static void main(String[] args) {
        String a = "有内鬼，中止交易";
        String s = encodeBase64(a);
        System.out.println(s);
        String s1 = decodeBase64(s);
        System.out.println(s1);
    }

}