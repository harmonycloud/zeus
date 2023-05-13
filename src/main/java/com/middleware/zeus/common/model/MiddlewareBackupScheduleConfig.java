package com.middleware.zeus.common.model;

import lombok.Data;

/**
 * 中间件备份规则
 * @author liyinlong
 * @since 2021/9/14 3:16 下午
 */
@Data
public class MiddlewareBackupScheduleConfig {

    /**
     * 是否支持停止备份
     */
    private Boolean canPause;

    /**
     * cron表达式
     */
    private String cron;

    /**
     * 备份保留数量
     */
    private Integer limitRecord;

    /**
     * 预计下次备份时间(废弃的字段)
     */
    @Deprecated
    private String nextBackupTime;

    /**
     * 是否开启备份（on / off）
     */
    private String pause;

    /**
     * 备份资源名称，服务名称或pod名称
     */
    private String sourceName;

    /**
     * 服务别名（服务级别的备份）
     */
    private String aliasName;

    /**
     * 备份类型，Cluster或Pod
     */
    private String backupType;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 备份规则名称
     */
    private String backupScheduleName;

    /**
     * pod角色（如果是pod级别备份）
     */
    private String podRole;

    /**
     * 最近一次执行时间
     */
    private String lastBackupTime;

    public MiddlewareBackupScheduleConfig() {
    }

    public MiddlewareBackupScheduleConfig(Boolean canPause, String cron, Integer limitRecord, String pause, String sourceName, String backupType, String createTime) {
        this.canPause = canPause;
        this.cron = cron;
        this.limitRecord = limitRecord;
        this.pause = pause;
        this.sourceName = sourceName;
        this.backupType = backupType;
        this.createTime = createTime;
        this.backupScheduleName = backupScheduleName;
    }

    public MiddlewareBackupScheduleConfig(String backupScheduleName, Boolean canPause, String cron, Integer limitRecord, String pause, String sourceName, String backupType, String createTime, String podRole) {
        this.canPause = canPause;
        this.cron = cron;
        this.limitRecord = limitRecord;
        this.pause = pause;
        this.sourceName = sourceName;
        this.backupType = backupType;
        this.createTime = createTime;
        this.backupScheduleName = backupScheduleName;
        this.podRole = podRole;
    }

}
