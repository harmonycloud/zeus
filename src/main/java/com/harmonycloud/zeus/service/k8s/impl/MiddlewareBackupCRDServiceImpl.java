package com.harmonycloud.zeus.service.k8s.impl;

import com.harmonycloud.caas.common.model.middleware.MiddlewareBackup;
import com.harmonycloud.zeus.integration.cluster.MiddlewareBackupWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupCRD;
import com.harmonycloud.zeus.service.k8s.MiddlewareBackupCRDService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author liyinlong
 * @since 2021/9/14 10:50 上午
 */
@Service
@Slf4j
public class MiddlewareBackupCRDServiceImpl implements MiddlewareBackupCRDService {

    @Autowired
    private MiddlewareBackupWrapper middlewareBackupWrapper;

    @Override
    public void create(String clusterId, MiddlewareBackupCRD middlewareBackupCRD) throws IOException {
        middlewareBackupWrapper.create(clusterId, middlewareBackupCRD);
    }

    @Override
    public void update(String clusterId, MiddlewareBackupCRD middlewareBackupCRD) throws IOException {
        middlewareBackupWrapper.update(clusterId, middlewareBackupCRD);
    }

    @Override
    public MiddlewareBackupCRD get(String clusterId, String namespace, String backupName) {
        return middlewareBackupWrapper.get(clusterId, namespace, backupName);
    }
}
