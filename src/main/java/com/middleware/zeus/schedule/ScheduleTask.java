package com.middleware.zeus.schedule;


import com.middleware.zeus.service.middleware.AlertRecordService;
import com.middleware.zeus.service.middleware.MiddlewareBackupService;
import com.middleware.zeus.service.system.AlertService;
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
    @Qualifier("middlewareBackupServiceImpl")
    @Autowired
    private MiddlewareBackupService middlewareBackupService;
    @Autowired
    private AlertRecordService alertRecordService;
    @Autowired
    private AlertService alertService;

    @Scheduled(fixedDelayString = "${system.license.refresh:60000}", initialDelay = 10 * 1000)
    public void calculateCpu() {
        try {
            licenseService.lockRefreshLicense();
        } catch (Exception e){
            log.error("刷新中间件资源使用情况失败");
            log.debug("刷新中间件资源使用情况失败, e");
        }
    }

    @Scheduled(cron = "${system.alert.record.cron:0 0 0 * * ?}")
    public void alertRecordClear() {
        try {
            alertRecordService.clear();
        } catch (Exception e){
            log.error("清理过期告警记录失败");
            log.error("清理过期告警记录失败", e);
        }
    }

    @Scheduled(cron = "0 */5 * ? * *")
    public void incBackupScheduleCheck() {
        try {
            middlewareBackupService.checkSchedule();
        } catch (Exception e){
            log.error("定时查询增量周期备份失败");
            log.debug("定时查询增量周期备份失败", e);
        }
    }

    @Scheduled(cron = "0 */5 * ? * *")
    public void clearRecycleFailedBackup() {
        try {
            middlewareBackupService.clearRecycleFailedBackup();
        } catch (Exception e){
            log.error("清理回收失败的备份失败");
            log.debug("清理回收失败的备份失败", e);
        }
    }

    @Scheduled(cron = "0 0 * ? * *")
    public void refreshPrometheusRulesLabels() {
        try {
            alertService.refreshPrometheusRulesLabels();
        } catch (Exception e){
            log.error("刷新prometheus规则标签失败");
            log.debug("刷新prometheus规则标签失败", e);
        }
    }

}
