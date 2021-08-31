package com.harmonycloud.zeus.config;

import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.util.K8sClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author liyinlong
 * @description 初始化es模板
 * @date 2021/7/7 10:20 上午
 */
@Component
@Slf4j
public class InitClusterInfo implements ApplicationRunner {

    @Autowired
    private K8sClient k8sClient;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("初始化集群信息");
            k8sClient.initClients();
            log.info("初始化集群信息成功");
        } catch (Exception e) {
            log.error("初始化集群信息失败");
        }
    }

}