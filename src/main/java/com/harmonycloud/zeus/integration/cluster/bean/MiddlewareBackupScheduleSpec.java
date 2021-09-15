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

    public MiddlewareBackupScheduleSpec() {
    }

    public MiddlewareBackupScheduleSpec(String name, String cron, Integer limitRecord) {
        this.name = name;
        if (StringUtils.isNotBlank(cron)) {
            this.cron = cron;
            this.limitRecord = limitRecord;
        }
    }
}

