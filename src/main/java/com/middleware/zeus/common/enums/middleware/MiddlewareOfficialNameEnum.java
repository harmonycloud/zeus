package com.middleware.zeus.common.enums.middleware;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liyinlong
 * 中间件标准名称枚举类，标准名称用于服务列表菜单
 * @since 2021/11/3 3:13 下午
 */
public enum MiddlewareOfficialNameEnum {

    /**
     * 中间件在menu中的名称映射
     */
    MYSQL("MYSQL", "MySQL"),
    REDIS("REDIS", "Redis"),
    ELASTICSEARCH("ELASTICSEARCH", "Elasticsearch"),
    ROCKETMQ("ROCKETMQ", "RocketMQ"),
    ZOOKEEPER("ZOOKEEPER", "ZooKeeper"),
    NACOS("NACOS", "Nacos"),
    KAFKA("KAFKA", "Kafka"),
    LOGSTASH("LOGSTASH", "Logstash"),
    MINIO("MINIO", "Minio"),
    HARBOR("HARBOR", "Harbor"),
    POSTGRESQL("POSTGRESQL", "PostgreSQL"),
    INGRESSNGINX("INGRESS-NGINX","Ingress-Nginx")
    ;

    private final String upperName;
    private final String officialName;

    private static final Map<String, String> typeMap = new HashMap<>();

    static {
        for (MiddlewareOfficialNameEnum typeEnum : MiddlewareOfficialNameEnum.values()) {
            typeMap.put(typeEnum.upperName, typeEnum.officialName);
        }
    }

    MiddlewareOfficialNameEnum(String upperName, String officialName) {
        this.upperName = upperName;
        this.officialName = officialName;
    }

    public static String findByChartName(String chartName) {
        String upperName = chartName.toUpperCase();
        String officialName = typeMap.get(upperName);
        if (officialName != null) {
            return officialName;
        }
        String firstLetter = String.valueOf(chartName.charAt(0));
        String upperFirstLetter = firstLetter.toUpperCase();
        chartName = chartName.replaceFirst(firstLetter, upperFirstLetter);
        return chartName;
    }

}
