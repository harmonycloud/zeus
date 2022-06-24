package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class MiddlewareBackupScheduleSpec {

    /**
     * 地址信息
     */
    private MiddlewareBackupScheduleDestination backupDestination;

    /**
     * 备份名称
     */
    private String name;

    /**
     * 中间件类型
     */
    private String type;


    /**
     * 是否开启备份
     */
    private String pause;

    /**
     * 定时备份设置
     */
    private Schedule schedule;

    private List<Map<String, List<String>>> customBackups;

    public MiddlewareBackupScheduleSpec() {
    }

    public MiddlewareBackupScheduleSpec(MiddlewareBackupScheduleDestination backupDestination, List<Map<String, List<String>>> customBackups, String name, String type, String pause, String cron, Integer limitRecord) {
        this.backupDestination = backupDestination;
        this.customBackups = customBackups;
        this.name = name;
        this.type = type;
        this.pause = pause;
        if (StringUtils.isNotBlank(cron)) {
            this.schedule = new Schedule(cron, limitRecord);
        }
    }

    @Data
    public static class MiddlewareBackupScheduleDestination {

        private String destinationType;

        private MiddlewareBackupParameters parameters;

        public MiddlewareBackupScheduleDestination() {

        }

        @Data
        public static class MiddlewareBackupParameters {

            private String bucket;

            private String url;

            private String bucketSubPath;

            private String userId;

            private String userKey;

            private String backupPassword;

            public MiddlewareBackupParameters(String bucket, String url, String bucketSubPath, String userId, String userKey, String backupPassword) {
                this.bucket = bucket;
                this.url = url;
                this.bucketSubPath = bucketSubPath;
                this.userId = userId;
                this.userKey = userKey;
                this.backupPassword = backupPassword;
            }

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
         * 备份保留时间
         */
        private String retentionTime;

        public Schedule() {
        }

        public Schedule(String cron, Integer limitRecord) {
            this.cron = cron;
            this.limitRecord = limitRecord;
        }
    }

}

