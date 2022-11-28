package com.harmonycloud.zeus.util;

/**
 * @author liyinlong
 * @since 2022/11/24 10:40 上午
 */
public class K8sServiceNameUtil {

    public static String getServicePath(String namespace, String middlewareName) {
        return middlewareName + "." + namespace;
    }

    public static String getServicePath(String prefix, String namespace, String middlewareName) {
        return prefix + "." + getServicePath(middlewareName, namespace);
    }

    public static String getRedisPredixyServicePath(String namespace, String middlewareName) {
        return middlewareName + "-predixy." + namespace;
    }

}
