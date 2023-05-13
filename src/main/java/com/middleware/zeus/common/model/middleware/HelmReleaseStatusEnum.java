package com.middleware.zeus.common.model.middleware;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public enum HelmReleaseStatusEnum {
    /**
     * helm发布状态
     */
    DEPLOYED("deployed",  "发布成功"),
    FAILED("failed", "发布失败"),
    UNINSTALLED("uninstalled", "已卸载"),
    UNINSTALLING("uninstalling",  "卸载中"),
    SUPERSEDED("superseded",  "升级中"),
    PENDING("pending", "等待中"),
    UNKNOWN("unknown", "未知");

    private final String code;
    private final String name;

    /**
     * 存放所有的code和Enum的转换
     */
    private static final Map<String, HelmReleaseStatusEnum> K8S_MODULE_MAP =
        new ConcurrentHashMap<>(HelmReleaseStatusEnum.values().length);

    static {
        /**
         * 将所有的实体类放入到map中，提供查询
         */
        for (HelmReleaseStatusEnum type : EnumSet.allOf(HelmReleaseStatusEnum.class)) {
            K8S_MODULE_MAP.put(type.getCode(), type);
        }
    }

    HelmReleaseStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Map<String, HelmReleaseStatusEnum> getModuleMap() {
        return K8S_MODULE_MAP;
    }

    public static HelmReleaseStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        return K8S_MODULE_MAP.get(code);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

}
