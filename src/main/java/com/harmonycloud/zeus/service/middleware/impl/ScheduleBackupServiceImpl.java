package com.harmonycloud.zeus.service.middleware.impl;

import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.caas.common.model.middleware.ScheduleBackup;
import com.harmonycloud.zeus.integration.cluster.ScheduleBackupWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.ScheduleBackupCRD;
import com.harmonycloud.zeus.service.middleware.ScheduleBackupService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2021/4/7 2:04 下午
 */
@Slf4j
@Service
public class ScheduleBackupServiceImpl implements ScheduleBackupService {

    @Autowired
    private ScheduleBackupWrapper scheduleBackupWrapper;

    /**
     * 查询备份列表
     *
     * @param clusterId
     * @param namespace
     * @param name
     * @return List<BackupDto>
     */
    @Override
    public List<ScheduleBackup> listScheduleBackup(String clusterId, String namespace, String name) {
        List<ScheduleBackupCRD> scheduleBackupCRDList = scheduleBackupWrapper.list(clusterId, namespace);
        if (CollectionUtils.isEmpty(scheduleBackupCRDList)) {
            return null;
        }
        scheduleBackupCRDList = scheduleBackupCRDList.stream()
                .filter(scheduleBackup -> scheduleBackup.getSpec().getBackupTemplate().getClusterName().equals(name))
                .collect(Collectors.toList());

        List<ScheduleBackup> scheduleBackupList = new ArrayList<>();
        scheduleBackupCRDList.forEach(scheduleBackupCRD -> {
            ScheduleBackup scheduleBackup = new ScheduleBackup().setName(scheduleBackupCRD.getMetadata().getName())
                .setNamespace(scheduleBackupCRD.getMetadata().getNamespace())
                .setControllerName(scheduleBackupCRD.getMetadata().getLabels().get("controllername"))
                .setMiddlewareCluster(scheduleBackupCRD.getSpec().getBackupTemplate().getClusterName())
                .setSchedule(scheduleBackupCRD.getSpec().getSchedule())
                .setKeepBackups(scheduleBackupCRD.getSpec().getKeepBackups());
            if (!ObjectUtils.isEmpty(scheduleBackupCRD.getStatus())) {
                scheduleBackup.setLastBackupName(scheduleBackupCRD.getStatus().getLastBackupName())
                    .setLastBackupFileName(scheduleBackupCRD.getStatus().getLastBackupFileName())
                    .setLastBackupTime(scheduleBackupCRD.getStatus().getLastBackupTime())
                    .setLastBackupPhase(scheduleBackupCRD.getStatus().getLastBackupPhase());
            }
            scheduleBackupList.add(scheduleBackup);
        });
        return scheduleBackupList;
    }

    /**
     * 创建定时备份
     *
     * @param clusterId
     * @param scheduleBackupCRD
     * @return
     */
    @Override
    public void create(String clusterId, ScheduleBackupCRD scheduleBackupCRD) {
        try {
            scheduleBackupWrapper.create(clusterId, scheduleBackupCRD);
        } catch (IOException e) {
            log.error("备份{}创建失败", scheduleBackupCRD.getMetadata().getName());
            throw new CaasRuntimeException(ErrorMessage.CREATE_BACKUP_FAILED);
        }
    }

    @Override
    public void delete(String clusterId, String namespace, String name) {
        List<ScheduleBackupCRD> crdList = scheduleBackupWrapper.list(clusterId, namespace);
        if (CollectionUtils.isEmpty(crdList)) {
            return;
        }
        crdList.forEach(crd -> {
            if (crd.getSpec().getBackupTemplate() == null
                || !StringUtils.equals(crd.getSpec().getBackupTemplate().getClusterName(), name)) {
                return;
            }
            try {
                scheduleBackupWrapper.delete(clusterId, namespace, crd.getMetadata().getName());
            } catch (IOException e) {
                log.error("集群：{}，命名空间：{}，中间件：{}，定时备份删除失败", clusterId, namespace, name, e);
            }
        });
    }

}
