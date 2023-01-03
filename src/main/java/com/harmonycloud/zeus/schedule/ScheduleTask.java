package com.harmonycloud.zeus.schedule;

import com.harmonycloud.zeus.service.log.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author xutianhong
 * @Date 2023/1/3 10:24 上午
 */
@Component
@EnableScheduling
@Slf4j
public class ScheduleTask {

    @Autowired
    private LogService logService;

    @Scheduled(cron = "${es.log.cron:0 0 0 * * ?}")
    public void logCleanSchedule() {
        try {
            logService.cleanHistoryLog();
        } catch (Exception e){
            log.error("定时清理日志失败", e);
        }
    }

}
