package com.harmonycloud.zeus.util;

/**
 * @author liyinlong
 * @since 2022/11/24 10:40 上午
 */
public class K8sServiceNameUtil {

    public static String getServicePath(String serviceName, String namespace) {
        return serviceName + "." + namespace;
    }

    public static String getServicePath(String prefix, String serviceName, String namespace) {
        return prefix + "." + getServicePath(serviceName, namespace);
    }

}
