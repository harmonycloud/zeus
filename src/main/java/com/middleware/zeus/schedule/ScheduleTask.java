package com.middleware.zeus.schedule;

import com.middleware.zeus.service.log.LogService;
import com.middleware.zeus.service.system.LicenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author xutianhong
 * @Date 2022/10/28 10:26 上午
 */
@Component
@EnableScheduling
@Slf4j
public class ScheduleTask {

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private LogService logService;

    @Scheduled(fixedDelayString = "${system.license.refresh:30000}", initialDelay = 10 * 1000)
    public void calculateCpu() throws Exception{
        //licenseService.refreshMiddlewareResource();
    }

    @Scheduled(cron = "${es.log.cron:0 0 0 * * ?}")
    public void logCleanSchedule() {
        try {
            logService.cleanHistoryLog();
        } catch (Exception e){
            log.error("定时清理日志失败", e);
        }
    }

}
