package com.middleware.zeus.common.enums.middleware;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Slf4j
public enum MiddlewareTypeEnum {

    /**
     * 默认为基础的中间件处理方式
     */
    BASE("base"),
    MYSQL("mysql"),
    REDIS("redis"),
    ELASTIC_SEARCH("elasticsearch"),
    ROCKET_MQ("rocketmq"),
    KAFKA("kafka"),
    ZOOKEEPER("zookeeper"),
    POSTGRESQL("postgresql"),
    ;

    /**
     * 默认为基础的中间件处理方式
     */

    private final String type;
    
    private static final Map<String, MiddlewareTypeEnum> typeEnumMap = new HashMap<>();
    
    static {
        for (MiddlewareTypeEnum typeEnum : MiddlewareTypeEnum.values()) {
            typeEnumMap.put(typeEnum.getType(), typeEnum);
        }
    }

    public static MiddlewareTypeEnum findByType(String type) {
        if (StringUtils.isEmpty(type) || typeEnumMap.get(type) == null) {
            log.warn("传入的中间件类型为:{}，将使用基础的中间件处理", type);
            return BASE;
        }
        return typeEnumMap.get(type);
    }

    public static boolean isType(String type){
        return typeEnumMap.containsKey(type);
    }

    MiddlewareTypeEnum(String type) {
        this.type = type;
        //this.middlewareCrdType = middlewareCrdType;
    }

    public String getType() {
        return type;
    }
}
