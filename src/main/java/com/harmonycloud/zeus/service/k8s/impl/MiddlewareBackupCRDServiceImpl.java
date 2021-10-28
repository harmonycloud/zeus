package com.harmonycloud.zeus.service.k8s.impl;

import com.harmonycloud.zeus.integration.cluster.MiddlewareBackupWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupCRD;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupList;
import com.harmonycloud.zeus.service.k8s.MiddlewareBackupCRDService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2021/9/15 10:33 上午
 */

@Service
public class MiddlewareBackupCRDServiceImpl implements MiddlewareBackupCRDService {

    @Autowired
    private MiddlewareBackupWrapper middlewareBackupWrapper;

    @Override
    public void create(String clusterId, MiddlewareBackupCRD middlewareBackupCRD) throws IOException {
        middlewareBackupWrapper.create(clusterId, middlewareBackupCRD);
    }

    @Override
    public void delete(String clusterId, String namespace, String name) throws IOException {
        middlewareBackupWrapper.delete(clusterId, namespace, name);
    }

    @Override
    public MiddlewareBackupList list(String clusterId, String namespace, Map<String, String> labels) {
        return middlewareBackupWrapper.list(clusterId, namespace, labels);
    }

    @Override
    public MiddlewareBackupCRD get(String clusterId, String namespace, String name) throws IOException {
        return middlewareBackupWrapper.get(clusterId, namespace, name);
    }
}
