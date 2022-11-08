package com.harmonycloud.zeus.schedule;

import com.harmonycloud.zeus.service.system.LicenseService;
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
public class ScheduleTask {

    @Autowired
    private LicenseService licenseService;

    @Scheduled(fixedRate =30 * 1000)
    public void calculateCpu() throws Exception{
        //licenseService.middlewareResource();
    }

}
