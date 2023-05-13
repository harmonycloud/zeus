package com.middleware.zeus.common.enums.middleware;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2022/11/12 8:21 下午
 */
@Slf4j
public enum MiddlewareGrafanaNameEnum {


    BASIC("basic", "var-service"),
    MYSQL("mysql", "var-mc"),
    REDIS("redis", "var-redis"),
    ELASTIC_SEARCH("elasticsearch", "var-instance"),
    ROCKET_MQ("rocketmq", "var-instance"),
    POSTGRESQL("postgresql", "var-service"),
    ZOOKEEPER("zookeeper", "var-service"),
    KAFKA("kafka", "var-kafkacluster"),
    ;

    private final String type;
    private final String name;

    private static final Map<String, MiddlewareGrafanaNameEnum> map = new HashMap<>();

    static {
        for (MiddlewareGrafanaNameEnum grafanaNameEnum : MiddlewareGrafanaNameEnum.values()) {
            map.put(grafanaNameEnum.getType(), grafanaNameEnum);
        }
    }

    public static MiddlewareGrafanaNameEnum findByType(String type) {
        if (map.containsKey(type)) {
            return map.get(type);
        } else {
            return map.get("basic");
        }
    }

    MiddlewareGrafanaNameEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }



}
