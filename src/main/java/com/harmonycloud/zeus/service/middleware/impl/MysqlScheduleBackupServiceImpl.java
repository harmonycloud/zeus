package com.harmonycloud.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.caas.common.model.middleware.MiddlewareBackupNameDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareBackupRecord;
import com.harmonycloud.caas.common.model.middleware.ScheduleBackup;
import com.harmonycloud.zeus.bean.BeanMiddlewareBackupName;
import com.harmonycloud.zeus.dao.BeanMiddlewareBackupNameMapper;
import com.harmonycloud.zeus.integration.cluster.MysqlScheduleBackupWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.service.middleware.MysqlScheduleBackupService;
import com.harmonycloud.zeus.util.CronUtils;
import com.harmonycloud.zeus.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
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
public class MysqlScheduleBackupServiceImpl implements MysqlScheduleBackupService {

    @Autowired
    private MysqlScheduleBackupWrapper mysqlScheduleBackupWrapper;
    @Autowired
    private BeanMiddlewareBackupNameMapper middlewareBackupNameMapper;

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
        List<MysqlScheduleBackupCR> mysqlScheduleBackupCRList = mysqlScheduleBackupWrapper.list(clusterId, namespace);
        if (CollectionUtils.isEmpty(mysqlScheduleBackupCRList)) {
            return null;
        }
        mysqlScheduleBackupCRList = mysqlScheduleBackupCRList.stream()
                .filter(scheduleBackup -> scheduleBackup.getSpec().getBackupTemplate().getClusterName().equals(name))
                .collect(Collectors.toList());

        List<ScheduleBackup> scheduleBackupList = new ArrayList<>();
        mysqlScheduleBackupCRList.forEach(scheduleBackupCRD -> {
            ScheduleBackup scheduleBackup = new ScheduleBackup().setName(scheduleBackupCRD.getMetadata().getName())
                    .setNamespace(scheduleBackupCRD.getMetadata().getNamespace())
                    .setControllerName(scheduleBackupCRD.getMetadata().getLabels().get("controllername"))
                    .setMiddlewareCluster(scheduleBackupCRD.getSpec().getBackupTemplate().getClusterName())
                    .setSchedule(scheduleBackupCRD.getSpec().getSchedule())
                    .setKeepBackups(scheduleBackupCRD.getSpec().getKeepBackups())
                    .setCreationTimestamp(scheduleBackupCRD.getMetadata().getCreationTimestamp());
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

    public List<MiddlewareBackupRecord> listScheduleBackupRecord(String clusterId, String namespace, String name) {
        List<MysqlScheduleBackupCR> mysqlScheduleBackupCRList = mysqlScheduleBackupWrapper.list(clusterId, namespace);
        if (CollectionUtils.isEmpty(mysqlScheduleBackupCRList)) {
            return null;
        }
        List<MiddlewareBackupRecord> recordList = new ArrayList<>();
        mysqlScheduleBackupCRList.forEach(schedule -> {
            MysqlScheduleBackupStatus backupStatus = schedule.getStatus();
            MiddlewareBackupRecord backupRecord = new MiddlewareBackupRecord();
            String backupTime = DateUtil.utc2Local(schedule.getMetadata().getCreationTimestamp(), DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
            backupRecord.setNamespace(schedule.getMetadata().getNamespace());
            backupRecord.setBackupTime(backupTime);
            backupRecord.setBackupName(schedule.getMetadata().getName());
            MysqlScheduleBackupSpec spec = schedule.getSpec();
            String time = schedule.getSpec().getSchedule();
            backupRecord.setCron(time);
            Minio minio = spec.getBackupTemplate().getStorageProvider().getMinio();
            String position = "minio" + "(" + minio.getEndpoint() + "/" + minio.getBucketName() + ")";
            backupRecord.setPosition(position);
            if (!ObjectUtils.isEmpty(backupStatus)) {
                switch (backupStatus.getLastBackupPhase()) {
                    case "Creating":
                    case "Running":
                        backupRecord.setPhrase("Running");
                        break;
                    case "Complete":
                        backupRecord.setPhrase("Success");
                        break;
                    case "Failed":
                        backupRecord.setPhrase("Failed");
                        break;
                    default:
                        backupRecord.setPhrase("Unknown");
                }
            } else {
                backupRecord.setPhrase("Unknown");
            }
            backupRecord.setSourceType(schedule.getMetadata().getLabels().get("type"));
            backupRecord.setSourceName(schedule.getSpec().getBackupTemplate().getClusterName());
            String backupId = schedule.getMetadata().getLabels().get("backupId");
            backupRecord.setBackupId(backupId);
            backupRecord.setTaskName(getBackupName(clusterId, backupId).getBackupName());
            backupRecord.setAddressName(schedule.getMetadata().getLabels().get("addressId"));
            backupRecord.setCron(CronUtils.parseLocalCron(schedule.getSpec().getSchedule()));
            if (schedule.getSpec().getKeepBackups() == null) {
                backupRecord.setBackupMode("single");
            } else {
                backupRecord.setBackupMode("period");
            }
            recordList.add(backupRecord);
        });
        return recordList;
    }

    /**
     * 创建定时备份
     *
     * @param clusterId
     * @param mysqlScheduleBackupCR
     * @return
     */
    @Override
    public void create(String clusterId, MysqlScheduleBackupCR mysqlScheduleBackupCR) {
        try {
            mysqlScheduleBackupWrapper.create(clusterId, mysqlScheduleBackupCR);
        } catch (IOException e) {
            log.error("备份{}创建失败", mysqlScheduleBackupCR.getMetadata().getName());
            throw new CaasRuntimeException(ErrorMessage.CREATE_BACKUP_FAILED);
        }
    }

    @Override
    public void delete(String clusterId, String namespace, String name) {
        List<MysqlScheduleBackupCR> crdList = mysqlScheduleBackupWrapper.list(clusterId, namespace);
        if (CollectionUtils.isEmpty(crdList)) {
            return;
        }
        crdList.forEach(crd -> {
            if (crd.getSpec().getBackupTemplate() == null
                || !StringUtils.equals(crd.getMetadata().getName(), name)) {
                return;
            }
            try {
                mysqlScheduleBackupWrapper.delete(clusterId, namespace, crd.getMetadata().getName());
            } catch (IOException e) {
                log.error("集群：{}，命名空间：{}，中间件：{}，定时备份删除失败", clusterId, namespace, name, e);
            }
        });
    }

    @Override
    public void update(String clusterId, MysqlScheduleBackupCR mysqlScheduleBackupCR) {
        try {
            mysqlScheduleBackupWrapper.update(clusterId, mysqlScheduleBackupCR);
        } catch (IOException e) {
            log.error("备份{}创建失败", mysqlScheduleBackupCR.getMetadata().getName());
            throw new CaasRuntimeException(ErrorMessage.CREATE_BACKUP_FAILED);
        }
    }

    @Override
    public MysqlScheduleBackupCR get(String clusterId, String namespace, String backupScheduleName) {
        return mysqlScheduleBackupWrapper.get(clusterId, namespace, backupScheduleName);
    }

    public MiddlewareBackupNameDTO getBackupName(String clusterId, String backupId) {
        QueryWrapper<BeanMiddlewareBackupName> wrapper = new QueryWrapper<BeanMiddlewareBackupName>().eq("cluster_id", clusterId).eq("backup_id", backupId);
        BeanMiddlewareBackupName backupName = middlewareBackupNameMapper.selectOne(wrapper);
        MiddlewareBackupNameDTO backupNameDTO = new MiddlewareBackupNameDTO();
        if (!ObjectUtils.isEmpty(backupName)) {
            BeanUtils.copyProperties(backupName, backupNameDTO);
        }
        return backupNameDTO;
    }

}
