package com.middleware.zeus.service.k8s.impl;

import com.middleware.zeus.integration.cluster.MiddlewareBackupScheduleWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareBackupSchedule;
import com.middleware.zeus.integration.cluster.bean.MiddlewareBackupScheduleList;
import com.middleware.zeus.service.k8s.MiddlewareBackupScheduleCRDService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2021/9/14 10:50 上午
 */
@Service
@Slf4j
public class MiddlewareBackupScheduleCRDServiceImpl implements MiddlewareBackupScheduleCRDService {

    @Autowired
    private MiddlewareBackupScheduleWrapper middlewareBackupScheduleWrapper;

    @Override
    public void create(String clusterId, MiddlewareBackupSchedule middlewareBackupSchedule) throws IOException {
        middlewareBackupScheduleWrapper.create(clusterId, middlewareBackupSchedule);
    }

    @Override
    public void createOrReplace(String clusterId, MiddlewareBackupSchedule middlewareBackupSchedule) throws IOException {
        middlewareBackupScheduleWrapper.createOrReplace(clusterId, middlewareBackupSchedule);
    }

    @Override
    public void update(String clusterId, MiddlewareBackupSchedule middlewareBackupSchedule) throws IOException {
        middlewareBackupScheduleWrapper.update(clusterId, middlewareBackupSchedule);
    }

    @Override
    public MiddlewareBackupSchedule get(String clusterId, String namespace, String backupName) {
        return middlewareBackupScheduleWrapper.get(clusterId, namespace, backupName);
    }

    @Override
    public void delete(String clusterId, String namespace, String name) throws IOException {
        middlewareBackupScheduleWrapper.delete(clusterId, namespace, name);
    }

    @Override
    public MiddlewareBackupScheduleList list(String clusterId, String namespace) {
        return middlewareBackupScheduleWrapper.list(clusterId, namespace, null);
    }

    @Override
    public List<MiddlewareBackupSchedule> listByLabels(String clusterId, String namespace,
                                                       Map<String, String> labels) {
        MiddlewareBackupScheduleList middlewareBackupScheduleList =
            middlewareBackupScheduleWrapper.list(clusterId, namespace, labels);
        if (middlewareBackupScheduleList != null && !CollectionUtils.isEmpty(middlewareBackupScheduleList.getItems())) {
            return middlewareBackupScheduleList.getItems();
        }
        return new ArrayList<>();
    }
}
