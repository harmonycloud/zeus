package com.harmonycloud.zeus.service.middleware.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.BackupType;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.MiddlewareBackupDTO;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.bean.BeanMiddlewareBackupName;
import com.harmonycloud.zeus.dao.BeanMiddlewareBackupNameMapper;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.operator.BaseOperator;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRService;
import com.harmonycloud.zeus.service.middleware.BackupService;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupAddressService;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupService;
import com.harmonycloud.zeus.service.middleware.MysqlScheduleBackupService;
import com.harmonycloud.zeus.util.CronUtils;
import com.harmonycloud.zeus.util.DateUtil;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;

/**
 * mysql备份
 *
 * @author liyinlong
 * @since 2021/10/26 4:16 下午
 */

@Slf4j
@Service
public class MysqlBackupServiceImpl implements MiddlewareBackupService {

    @Autowired
    private BackupService backupService;
    @Autowired
    private MiddlewareCRService middlewareCRService;
    @Autowired
    private MysqlScheduleBackupService mysqlScheduleBackupService;
    @Autowired
    private MiddlewareServiceImpl middlewareService;
    @Autowired
    private MiddlewareBackupAddressService middlewareBackupAddressService;
    @Autowired
    private BeanMiddlewareBackupNameMapper middlewareBackupNameMapper;

    @Override
    public List<MiddlewareBackupRecord> listBackup(String clusterId, String namespace, String middlewareName,
        String type) {
        List<Backup> backupList = backupService.listBackup(clusterId, namespace);
        return convertMysqlBackupDto(backupList, clusterId);
    }

    private List<MiddlewareBackupRecord> convertMysqlBackupDto(List<Backup> backupList, String clusterId) {
        List<MiddlewareBackupRecord> list = new ArrayList<>();
        for (Backup backup : backupList) {
            MiddlewareBackupRecord record = new MiddlewareBackupRecord();
            record.setNamespace(backup.getNamespace());
            record.setSourceName(backup.getName());
            record.setBackupType(BackupType.CLUSTER.getType());
            record.setBackupName(backup.getBackupName());
            record.setBackupId(backup.getBackupId());
            record.setCrName(backup.getCrName());
            record.setBackupFileName(backup.getBackupFileName());
            record.setSourceType(MiddlewareTypeEnum.MYSQL.getType());
            record.setOwner(backup.getOwner());
            record.setSchedule(false);
            // 获取备份时间
            if (backup.getBackupTime() != null) {
                String backupTime = DateUtil.utc2Local(DateUtils.parseUTCDate(backup.getBackupTime()),
                    DateType.YYYY_MM_DD_HH_MM_SS.getValue());
                record.setBackupTime(backupTime);
            } else {
                record.setBackupTime("/");
            }
            if (backup.getPhase() != null) {
                switch (backup.getPhase()) {
                    case "Creating":
                    case "Running":
                        record.setPhrase("Running");
                        break;
                    case "Complete":
                        record.setPhrase("Success");
                        break;
                    case "Failed":
                        record.setPhrase("Failed");
                        break;
                    default:
                        record.setPhrase("Unknown");
                }
            } else {
                record.setPhrase("Unknown");
            }
            record.setPosition("minio(" + backup.getEndPoint() + "/" + backup.getBucketName() + ")");
            record.setAddressId(backup.getAddressId());
            record.setBackupMode("single");

            // 标记使用mysqlBackup
            record.setMysqlBackup(true);
            list.add(record);
        }
        return list;
    }

    @Override
    public void createBackup(MiddlewareBackupDTO backupDTO) {
        if (StringUtils.isBlank(backupDTO.getCron())) {
            createNormalBackup(backupDTO);
        } else {
            createBackupSchedule(backupDTO);
        }
    }

    @Override
    public void createIncBackup(String clusterId, String namespace, String backupName, String time) {

    }

    @Override
    public void updateBackupSchedule(MiddlewareBackupDTO backupDTO) {
        MysqlScheduleBackupCR backupCRD = mysqlScheduleBackupService.get(backupDTO.getClusterId(),
            backupDTO.getNamespace(), backupDTO.getBackupScheduleName());
        MysqlScheduleBackupSpec spec = backupCRD.getSpec();
        spec.setSchedule(CronUtils.parseUtcCron(backupDTO.getCron()));
        spec.setKeepBackups(backupDTO.getLimitRecord());
        mysqlScheduleBackupService.update(backupDTO.getClusterId(), backupCRD);
    }

    @Override
    public void deleteRecord(String clusterId, String namespace, String type, String backupName) {
        try {
            backupService.delete(clusterId, namespace, backupName);
            // minioWrapper.removeObject(getMinio(chineseName), backupName);
        } catch (Exception e) {
            log.error("删除备份失败", e);
        }
    }

    /**
     * 创建mysql备份(定时/周期)
     * 
     * @param backupDTO
     */
    @Override
    public void createBackupSchedule(MiddlewareBackupDTO backupDTO) {
        // 校验是否运行中
        Middleware middleware = convertBackupToMiddleware(backupDTO);
        middlewareCRService.getCRAndCheckRunning(middleware);

        Minio minio = getMinio(backupDTO.getAddressId());
        BackupTemplate backupTemplate = new BackupTemplate().setClusterName(backupDTO.getMiddlewareName())
            .setStorageProvider(new BackupStorageProvider().setMinio(minio));

        MysqlScheduleBackupSpec spec =
            new MysqlScheduleBackupSpec().setSchedule(CronUtils.parseUtcCron(backupDTO.getCron()))
                .setBackupTemplate(backupTemplate).setKeepBackups(backupDTO.getLimitRecord());
        ObjectMeta metaData = new ObjectMeta();
        metaData.setName(backupDTO.getMiddlewareName() + UUIDUtils.get8UUID());
        Map<String, String> labels = new HashMap<>();
        labels.put("controllername", "backup-schedule-controller");
        String backupId = UUIDUtils.get16UUID();
        labels.put("backupId", backupId);
        labels.put("addressId", backupDTO.getAddressId());
        labels.put("type", backupDTO.getType());
        metaData.setLabels(labels);
        metaData.setNamespace(backupDTO.getNamespace());
        metaData.setClusterName(backupDTO.getMiddlewareName());

        MysqlScheduleBackupCR mysqlScheduleBackupCR =
            new MysqlScheduleBackupCR().setKind("MysqlBackupSchedule").setSpec(spec).setMetadata(metaData);
        mysqlScheduleBackupService.create(backupDTO.getClusterId(), mysqlScheduleBackupCR);
        createBackupName(backupDTO.getClusterId(), backupDTO.getTaskName(), backupId, "schedule");
    }

    /**
     * 创建mysql备份
     * 
     * @param backupDTO
     */
    @Override
    public void createNormalBackup(MiddlewareBackupDTO backupDTO) {
        middlewareCRService.getCRAndCheckRunning(convertBackupToMiddleware(backupDTO));
        BackupSpec spec = new BackupSpec().setClusterName(backupDTO.getMiddlewareName())
            .setStorageProvider(new BackupStorageProvider().setMinio(getMinio(backupDTO.getAddressId())));
        ObjectMeta metaData = new ObjectMeta();
        metaData.setName(backupDTO.getMiddlewareName() + "-" + UUIDUtils.get8UUID());
        Map<String, String> labels = new HashMap<>(1);
        labels.put("controllername", "backup-controller");
        String backupId = UUIDUtils.get16UUID();
        labels.put("backupId", backupId);
        labels.put("addressId", backupDTO.getAddressId());
        labels.put("type", backupDTO.getType());
        metaData.setLabels(labels);
        metaData.setNamespace(backupDTO.getNamespace());
        metaData.setClusterName(backupDTO.getMiddlewareName());
        BackupCR backupCR = new BackupCR().setKind("MysqlBackup").setSpec(spec).setMetadata(metaData);
        backupService.create(backupDTO.getClusterId(), backupCR);
        createBackupName(backupDTO.getClusterId(), backupDTO.getTaskName(), backupId, "normal");
    }

    @Override
    public void createRestore(String clusterId, String namespace, String middlewareName, String type, String backupName) {
    }

    @Override
    public void deleteMiddlewareBackupInfo(String clusterId, String namespace, String type, String middlewareName) {
        List<Backup> backupList = backupService.listBackup(clusterId, namespace);
        backupList.forEach(backup -> {
            if (!backup.getName().contains(middlewareName)) {
                return;
            }
            try {
                deleteRecord(clusterId, namespace, type, backup.getName());
            } catch (Exception e) {
                log.error("集群：{}，命名空间：{}，mysql中间件：{}，删除mysql备份异常", e);
            }
        });
    }

    @Override
    public List<MiddlewareBackupRecord> listBackupSchedule(String clusterId, String namespace, String type,
        String middlewareName) {
        List<MiddlewareBackupRecord> recordList =
            mysqlScheduleBackupService.listScheduleBackupRecord(clusterId, namespace, middlewareName);
        if (CollectionUtils.isEmpty(recordList)) {
            return new ArrayList<>();
        }
        return recordList;
    }

    @Override
    public void deleteSchedule(String clusterId, String namespace, String type, String backupScheduleName) {
        mysqlScheduleBackupService.delete(clusterId, namespace, backupScheduleName);
    }

    @Override
    public boolean checkIfAlreadyBackup(String clusterId, String namespace, String type, String middlewareName) {
        List<ScheduleBackup> scheduleBackupList =
            mysqlScheduleBackupService.listScheduleBackup(clusterId, namespace, middlewareName);
        if (scheduleBackupList != null && scheduleBackupList.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean checkIfAlreadyBackup(String clusterId, String namespace, String type, String middlewareName,
        String podName) {
        return false;
    }

    @Override
    public List<MiddlewareBackupRecord> backupTaskList(String clusterId, String namespace, String middlewareName,
        String type, String keyword) {
        return null;
    }

    @Override
    public MiddlewareBackupRecord getBackupTask(String clusterId, String namespace, String backupName) {
        return null;
    }

    @Override
    public List<MiddlewareBackupRecord> backupRecords(String clusterId, String namespace, String middlewareName,
        String type) {
        return null;
    }

    @Override
    public void deleteBackUpTask(String clusterId, String namespace, String type, String backupName, String backupId,
        Boolean schedule) {

    }

    @Override
    public void deleteBackUpRecord(String clusterId, String namespace, String type, String crName, String backupId) {

    }

    @Override
    public void createBackupName(String clusterId, String taskName, String backupId, String backupType) {
        BeanMiddlewareBackupName backupName = new BeanMiddlewareBackupName();
        backupName.setBackupName(taskName);
        backupName.setBackupId(backupId);
        backupName.setClusterId(clusterId);
        backupName.setBackupType(backupType);
        middlewareBackupNameMapper.insert(backupName);
    }

    @Override
    public void deleteBackupName(String clusterId, String taskName, String backupType) {

    }

    private void tryCreateMiddleware(String clusterId, String namespace, String type, String middlewareName,
        Middleware middleware) {
        for (int i = 0; i < 600; i++) {
            if (!middlewareCRService.checkIfExist(clusterId, namespace, type, middlewareName)) {
                middlewareService.create(middleware);
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("mysql恢复创建失败", e);
            }
        }
    }

    public Minio getMinio(String addressId) {
        List<MiddlewareClusterBackupAddressDTO> backupAddressDTOS =
            middlewareBackupAddressService.listBackupAddress(addressId, null);
        Minio minio = new Minio();
        if (!CollectionUtils.isEmpty(backupAddressDTOS)) {
            BeanUtils.copyProperties(backupAddressDTOS.get(0), minio);
        }
        if (minio.getBucketName().indexOf("/") == 0) {
            minio.setBucketName(minio.getBucketName().substring(1));
        }
        return minio;
    }

    /**
     * 将备份信息转为middleware对象
     *
     * @param backupDTO
     * @return
     */
    private Middleware convertBackupToMiddleware(MiddlewareBackupDTO backupDTO) {
        return new Middleware().setClusterId(backupDTO.getClusterId()).setNamespace(backupDTO.getNamespace())
            .setType(backupDTO.getType()).setName(backupDTO.getMiddlewareName());
    }

    /**
     * 修复存储单位
     *
     * @param middleware
     */
    public void fixStorageUnit(Middleware middleware) {
        Map<String, MiddlewareQuota> quota = middleware.getQuota();
        if (quota != null) {
            quota.forEach((k, v) -> {
                if (v.getStorageClassQuota() != null) {
                    v.setStorageClassQuota(v.getStorageClassQuota().replaceAll("Gi", ""));
                }
            });
        }
    }

}
