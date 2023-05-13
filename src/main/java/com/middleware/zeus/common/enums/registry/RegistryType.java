package com.middleware.zeus.common.enums.registry;

import org.apache.commons.lang3.StringUtils;

public enum RegistryType {
    HARBOR("harbor"),
    JFROG("jfrog"),
    OTHER("other"),
    ;

    private String type;

    RegistryType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static boolean contains(String type) {
        for (RegistryType enums : RegistryType.values()) {
            if (enums.getType().equals(type.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否自有类型
     *
     * @param type 类型
     * @return
     */
    public static boolean isSelfType(String type) {
        return StringUtils.equals(HARBOR.getType(), type) || StringUtils.equals(JFROG.getType(), type);
    }

}
