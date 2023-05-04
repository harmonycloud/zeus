package com.middleware.zeus.service.k8s.impl;

import com.middleware.zeus.integration.cluster.MiddlewareBackupWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareBackup;
import com.middleware.zeus.service.k8s.MiddlewareBackupCRService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2021/9/15 10:33 上午
 */

@Service
public class MiddlewareBackupCRServiceImpl implements MiddlewareBackupCRService {

    @Autowired
    private MiddlewareBackupWrapper middlewareBackupWrapper;

    @Override
    public void create(String clusterId, MiddlewareBackup middlewareBackup) throws IOException {
        middlewareBackupWrapper.create(clusterId, middlewareBackup);
    }

    @Override
    public void delete(String clusterId, String namespace, String name) throws IOException {
        middlewareBackupWrapper.delete(clusterId, namespace, name);
    }

    @Override
    public List<MiddlewareBackup> list(String clusterId, String namespace, Map<String, String> labels) {
        return middlewareBackupWrapper.list(clusterId, namespace, labels);
    }

    @Override
    public List<MiddlewareBackup> list(String clusterId, String namespace) {
        return middlewareBackupWrapper.list(clusterId, namespace, null);
    }

    @Override
    public MiddlewareBackup get(String clusterId, String namespace, String name) {
        return middlewareBackupWrapper.get(clusterId, namespace, name);
    }
}
