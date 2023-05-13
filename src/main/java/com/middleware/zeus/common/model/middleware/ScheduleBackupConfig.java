package com.middleware.zeus.common.model.middleware;

import lombok.Data;

import java.util.Date;

/**
 * @author xutianhong
 * @Date 2021/4/6 10:45 上午
 */
@Data
public class ScheduleBackupConfig {

    private Integer keepBackups;

    private String cron;

    private Date nextBackupDate;
}
