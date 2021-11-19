package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Data
@Accessors(chain = true)
public class MiddlewareBackupScheduleSpec {

    /**
     * 备份名称
     */
    private String name;

    /**
     * 中间件类型
     */
    private String type;

    /**
     * 备份信息
     */
    private List<BackupObject> backupObjects;

    /**
     * 备份存储
     */
    private String backendStorage;

    /**
     * 是否开启备份
     */
    private String pause;

    /**
     * 定时备份设置
     */
    private Schedule schedule;

    public MiddlewareBackupScheduleSpec() {
    }

    public MiddlewareBackupScheduleSpec(String name, String type,String cron, Integer limitRecord, List<BackupObject> backupObjects) {
        this.name = name;
        this.type = type;
        this.backupObjects = backupObjects;
        if (StringUtils.isNotBlank(cron)) {
            this.schedule = new Schedule(cron, limitRecord);
        }
    }

    @Data
    public static class Schedule {

        /**
         * cron表达式
         */
        private String cron;

        /**
         * 备份保留个数
         */
        private Integer limitRecord;

        /**
         * 开始时间
         */
        private String startTime;

        public Schedule() {
        }

        public Schedule(String cron, Integer limitRecord) {
            this.cron = cron;
            this.limitRecord = limitRecord;
        }
    }

}

