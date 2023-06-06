package com.middleware.zeus.schedule;

import com.middleware.zeus.service.log.LogService;
import com.middleware.zeus.service.middleware.MiddlewareBackupService;
import com.middleware.zeus.service.system.LicenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    @Qualifier("middlewareBackupServiceImpl")
    @Autowired
    private MiddlewareBackupService middlewareBackupService;

    @Scheduled(fixedDelayString = "${system.license.refresh:30000}", initialDelay = 10 * 1000)
    public void calculateCpu() {
        try {
            licenseService.refreshMiddlewareResource();
        } catch (Exception e){
            log.error("刷新中间件资源使用情况失败");
            log.debug("刷新中间件资源使用情况失败, e");
        }
    }

    @Scheduled(cron = "${es.log.cron:0 0 0 * * ?}")
    public void logCleanSchedule() {
//        try {
//            logService.cleanHistoryLog();
//        } catch (Exception e){
//            log.error("定时清理日志失败", e);
//        }
    }

    @Scheduled(cron = "0 */5 * ? * *")
    public void incBackupScheduleCheck() {
        try {
            log.info("开始同步查询增量备份任务");
            middlewareBackupService.checkSchedule();
        } catch (Exception e){
            log.error("定时查询增量周期备份失败");
            log.debug("定时查询增量周期备份失败", e);
        }
    }

}
