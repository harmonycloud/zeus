package com.harmonycloud.zeus.service.k8s.impl;

import com.harmonycloud.zeus.integration.cluster.MiddlewareRestoreWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareRestoreCRD;
import com.harmonycloud.zeus.service.k8s.MiddlewareRestoreCRDService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
    public void create(String clusterId, MiddlewareRestoreCRD middlewareRestoreCRD) throws IOException {
        middlewareRestoreWrapper.create(clusterId, middlewareRestoreCRD);
    }
}
