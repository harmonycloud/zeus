package com.middleware.zeus.config;

import com.middleware.zeus.service.k8s.ClusterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author liyinlong
 * @since 2022/7/7 4:51 下午
 */
@Component
@Slf4j
@ConditionalOnProperty(value="system.usercenter",havingValue = "skyview2")
public class SyncCluster {

    @Autowired
    private ClusterService clusterService;

    @PostConstruct
    public void init() throws Exception {
        clusterService.listClusters();
    }

}