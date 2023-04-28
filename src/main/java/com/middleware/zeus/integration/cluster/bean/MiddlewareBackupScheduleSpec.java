package com.middleware.zeus.integration.cluster.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
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
     * 开启/关闭备份
     */
    private String pause;

    /**
     * 定时备份设置
     */
    private Schedule schedule;

    private List<Map<String, List<Map<String, String>>>> customBackups;

    public MiddlewareBackupScheduleSpec(MiddlewareBackupScheduleDestination backupDestination, List<Map<String, List<Map<String, String>>>> customBackups, String name, String type, String pause, String cron, Integer limitRecord, Integer retentionTime) {
        this.backupDestination = backupDestination;
        this.customBackups = customBackups;
        this.name = name;
        this.type = type;
        this.pause = pause;
        if (StringUtils.isNotBlank(cron)) {
            this.schedule = new Schedule(cron, limitRecord, retentionTime);
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class MiddlewareBackupScheduleDestination {

        private String destinationType;

        private MiddlewareBackupParameters parameters;

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        public static class MiddlewareBackupParameters {

            private String bucket;

            private String url;

            private String bucketSubPath;

            private String userId;

            private String userKey;

            private String backupPassword;

        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
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
        private Integer retentionTime;

    }

}

