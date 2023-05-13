package com.middleware.zeus.common.enums;

/**
 * @description 备份服务器用途枚举类
 * @author  liyinlong
 * @since 2023/1/13 2:48 下午
 */
public enum ServerUsageEnum {

    zoneA("A", "可用区A"),
    zoneB("B", "可用区B"),
    ;

    private final String name;
    private final String aliasName;

    ServerUsageEnum(String name, String aliasName){
        this.name = name;
        this.aliasName = aliasName;
    }

    public String getName() {
        return name;
    }

    public String getAliasName() {
        return aliasName;
    }

}
