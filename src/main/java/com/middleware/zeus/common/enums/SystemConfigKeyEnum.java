package com.middleware.zeus.common.enums;

/**
 * @author xutianhong
 * @Date 2022/5/16 2:02 下午
 */
public enum SystemConfigKeyEnum {

    PASSWORD_EXPIRED_DAY("密码过期天数", "password_expired_day"),
    ;

    private final String name;
    private final String nameKey;


    SystemConfigKeyEnum(String name, String nameKey) {
        this.name = name;
        this.nameKey = nameKey;
    }

    public String getName() {
        return name;
    }

    public String getNameKey() {
        return nameKey;
    }

}
