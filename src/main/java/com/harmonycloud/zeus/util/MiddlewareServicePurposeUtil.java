package com.harmonycloud.zeus.util;

/**
 * @author liyinlong
 * @since 2022/7/14 2:14 下午
 */
public class MiddlewareServicePurposeUtil {

    public static String convertChinesePurpose(String middlewareType, String serviceName) {
        switch (middlewareType) {
            case "mysql":
                return convertMysql(serviceName);
            case "redis":
                return convertRedis(serviceName);
            case "elasticsearch":
                return convertEs(serviceName);
            case "postgresql":
                return convertPgSQL(serviceName);
            case "rocketmq":
                return convertRocketMQ(serviceName);
            case "kafka":
                return convertKafka(serviceName);
            case "zookeeper":
                return convertZookeeper(serviceName);
            default:
                return "/";
        }
    }

    public static String convertMysql(String serviceName) {
        if (serviceName.contains("readonly")) {
            return "只读";
        } else if (serviceName.contains("proxy")) {
            return "读写分离";
        } else {
            return "读写";
        }
    }

    public static String convertRedis(String serviceName) {
        if (serviceName.contains("readonly")) {
            return "只读";
        } else if (serviceName.contains("predixy")) {
            return "读写";
        } else {
            return "读写";
        }
    }

    public static String convertEs(String serviceName) {
        if (serviceName.contains("master") || serviceName.contains("data") || serviceName.contains("client")) {
            return "读写";
        } else if (serviceName.contains("kibana")) {
            return "管理页面";
        } else {
            return "/";
        }
    }

    public static String convertPgSQL(String serviceName) {
        if (serviceName.contains("repl")) {
            return "只读";
        } else {
            return "读写";
        }
    }

    public static String convertRocketMQ(String serviceName) {
        if (serviceName.contains("console-svc")) {
            return "管理页面";
        } else {
            return "服务连接";
        }
    }

    public static String convertKafka(String serviceName) {
        if (serviceName.contains("manager-svc")) {
            return "管理页面";
        } else {
            return "服务连接";
        }
    }

    public static String convertZookeeper(String serviceName) {
        if (serviceName.contains("client")) {
            return "服务连接";
        } else {
            return "/";
        }
    }

}
