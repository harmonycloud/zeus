package com.middleware.zeus.common.enums;

/**
 * @description 备份任务状态枚举类
 * @author  liyinlong
 * @since 2023/1/13 2:48 下午
 */
public enum BackupStatusEnum {

    RUNNING("Running", "进行中"),
    SUCCESS("Success", "成功"),
    FAILED("Failed", "失败"),
    CREATING("Creating", "创建中"),
    DELETING("Deleting", "删除中"),
    UNKNOWN("Unknown", "未知"),
    RECYCLEFAILED("RecycleFailed", "删除失败"),
    ;

    private final String status;
    private final String aliasName;

    BackupStatusEnum(String status, String aliasName){
        this.status = status;
        this.aliasName = aliasName;
    }

    public String getStatus() {
        return status;
    }

    public String getAliasName() {
        return aliasName;
    }

}
