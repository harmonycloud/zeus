package com.middleware.zeus.common.enums.middleware;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/5/11 2:18 下午
 */
@Slf4j
public enum MiddlewareConfigEnum {

    BASIC("basic", "config"),
    MYSQL("mysql", "config"),
    REDIS("redis", "config"),
    ELASTIC_SEARCH("elasticsearch", "es-config"),
    ROCKET_MQ("rocketmq", "rocketmq-config"),
    ;

    private final String type;
    private final String config;

    private static final Map<String, MiddlewareConfigEnum> map = new HashMap<>();

    static {
        for (MiddlewareConfigEnum configEnum : MiddlewareConfigEnum.values()) {
            map.put(configEnum.getType(), configEnum);
        }
    }

    public static MiddlewareConfigEnum findByType(String type) {
        if (map.containsKey(type)) {
            return map.get(type);
        } else {
            return map.get("basic");
        }
    }

    MiddlewareConfigEnum(String type, String config) {
        this.type = type;
        this.config = config;
    }

    public String getType() {
        return type;
    }

    public String getConfig() {
        return config;
    }

}
