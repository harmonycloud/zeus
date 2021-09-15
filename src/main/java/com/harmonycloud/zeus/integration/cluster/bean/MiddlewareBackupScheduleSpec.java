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
     * pod名称（选填）
     */
    private String pod;

    /**
     * pod存储pvc(选填)
     */
    private List<String> pvcs;

    /**
     * 备份存储
     */
    private String backendStorage;

    /**
     * 备份时间规则
     */
    private Schedule schedule;

    public MiddlewareBackupScheduleSpec() {
    }

    public MiddlewareBackupScheduleSpec(String name, String cron, Integer limitRecord) {
        this.name = name;
        if (StringUtils.isNotBlank(cron)) {
            this.schedule = new Schedule(cron, limitRecord, null);
        }
    }

    /**
     * 备份时间规则
     */
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

        public Schedule(String cron, Integer limitRecord, String startTime) {
            this.cron = cron;
            this.limitRecord = limitRecord;
            this.startTime = startTime;
        }
    }

}

