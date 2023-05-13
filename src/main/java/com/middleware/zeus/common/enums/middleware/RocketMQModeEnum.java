package com.middleware.zeus.common.enums.middleware;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * @author dengyulong
 * @date 2021/04/06
 */
public enum  RocketMQModeEnum {

    /**
     * mq模式
     */
    TWO_MASTER("2m-noslave","两主"),
    TWO_MASTER_TWO_SLAVE("2m-2s","两主两从"),
    THREE_MASTER_THREE_SLAVE("3m-3s","三主三从"),
    DLEDGER("dledger", "多副本"),
    ;

    private static final Map<String, RocketMQModeEnum> map = new HashMap<>();

    private final String mode;
    private final String name;

    static {
        for (RocketMQModeEnum modeEnum : RocketMQModeEnum.values()) {
            map.put(modeEnum.getMode(), modeEnum);
        }
    }

    public static RocketMQModeEnum findByMode(String mode) {
        if (StringUtils.isBlank(mode)) {
            throw new IllegalArgumentException("rocketmq mode is illegal");
        }
        RocketMQModeEnum modeEnum = map.get(mode);
        if (modeEnum == null) {
            throw new IllegalArgumentException("rocketmq mode is illegal");
        }
        return modeEnum;
    }

    RocketMQModeEnum(String mode, String name) {
        this.mode = mode;
        this.name = name;
    }

    public String getMode() {
        return mode;
    }

    public String getName() {
        return name;
    }
}
