package com.middleware.zeus.service.k8s.impl;

import com.middleware.zeus.integration.cluster.MiddlewareRestoreWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareRestoreCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareRestoreList;
import com.middleware.zeus.service.k8s.MiddlewareRestoreCRDService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2021/9/16 10:31 上午
 */
@Service
@Slf4j
public class MiddlewareRestoreCRDServiceImpl implements MiddlewareRestoreCRDService {

    @Autowired
    private MiddlewareRestoreWrapper middlewareRestoreWrapper;

    @Override
    public void create(String clusterId, MiddlewareRestoreCR middlewareRestoreCR) throws IOException {
        middlewareRestoreWrapper.create(clusterId, middlewareRestoreCR);
    }

    @Override
    public void delete(String clusterId, String namespace, String name) throws IOException {
        middlewareRestoreWrapper.delete(clusterId, namespace, name);
    }

    @Override
    public MiddlewareRestoreList list(String clusterId, String namespace, Map<String, String> labels) {
        return middlewareRestoreWrapper.list(clusterId, namespace, labels);
    }


}
