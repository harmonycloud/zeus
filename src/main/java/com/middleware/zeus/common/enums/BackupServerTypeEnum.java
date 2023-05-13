package com.middleware.zeus.common.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 备份介质类型枚举
 *
 * @author liyinlong
 * @since 2021/11/17 9:22 上午
 */
public enum BackupServerTypeEnum {

    S3(1, "S3"),
    FTP(2, "Ftp"),
    SERVER(3, "server");

    private Integer type;

    private String name;

    private static Map<Integer, BackupServerTypeEnum> serverMap = new HashMap<>();

    static {
        serverMap.put(S3.type, S3);
        serverMap.put(FTP.type, FTP);
        serverMap.put(SERVER.type, SERVER);
    }

    BackupServerTypeEnum(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static String findByType(Integer type) {
        return serverMap.get(type).name;
    }

}
