package com.harmonycloud.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.enums.BackupType;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.MiddlewareBackupDTO;
import com.harmonycloud.caas.common.model.MiddlewareBackupScheduleConfig;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.integration.minio.MinioWrapper;
import com.harmonycloud.zeus.operator.BaseOperator;
import com.harmonycloud.zeus.schedule.MiddlewareManageTask;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRService;
import com.harmonycloud.zeus.service.middleware.BackupService;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupService;
import com.harmonycloud.zeus.service.middleware.MysqlScheduleBackupService;
import com.harmonycloud.zeus.util.CronUtils;
import com.harmonycloud.zeus.util.DateUtil;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.NameConstant.STORAGE;

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
    private MinioWrapper minioWrapper;
    @Autowired
    private BackupService backupService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private MiddlewareCRService middlewareCRService;
    @Autowired
    private MysqlScheduleBackupService mysqlScheduleBackupService;
    @Autowired
    private MiddlewareServiceImpl middlewareService;
    @Autowired
    private MiddlewareManageTask middlewareManageTask;

    @Override
    public List<MiddlewareBackupRecord> listRecord(String clusterId, String namespace, String middlewareName, String type) {
        List<MysqlBackupDto> backups = listRecord(clusterId, namespace, middlewareName);
        List<MiddlewareBackupRecord> list = new ArrayList<>();
        for (MysqlBackupDto backup : backups) {
            MiddlewareBackupRecord record = new MiddlewareBackupRecord();
            record.setSourceName(middlewareName);
            record.setBackupType(BackupType.CLUSTER.getType());
            record.setBackupName(backup.getBackupName());
            record.setBackupFileName(backup.getBackupFileName());
            if (backup.getDate() != null) {
                String backupTime = DateUtil.utc2Local(backup.getDate(), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
                record.setBackupTime(backupTime);
            }else{
                record.setBackupTime("/");
            }
            if (backup.getStatus() != null) {
                switch (backup.getStatus()) {
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
            List<String> addressList = new ArrayList<>();
            addressList.add(backup.getPosition());
            record.setBackupAddressList(addressList);
            list.add(record);
        }
        return list;
    }

    @Override
    public BaseResult createBackup(MiddlewareBackupDTO backupDTO) {
        if (StringUtils.isBlank(backupDTO.getCron())) {
            createNormalBackup(backupDTO);
        } else {
            createBackupSchedule(backupDTO);
        }
        return BaseResult.ok();
    }

    @Override
    public BaseResult updateBackupSchedule(MiddlewareBackupDTO backupDTO) {
        MysqlScheduleBackupCR backupCRD = mysqlScheduleBackupService.get(backupDTO.getClusterId(), backupDTO.getNamespace(), backupDTO.getBackupScheduleName());
        MysqlScheduleBackupSpec spec = backupCRD.getSpec();
        spec.setSchedule(CronUtils.parseMysqlUtcCron(backupDTO.getCron()));
        spec.setKeepBackups(backupDTO.getLimitRecord());
        mysqlScheduleBackupService.update(backupDTO.getClusterId(), backupCRD);
        return BaseResult.ok();
    }

    @Override
    public BaseResult deleteRecord(String clusterId, String namespace, String middlewareName, String type, String backupName, String backupFileName) {
        try {
            backupService.delete(clusterId, namespace, backupName);
            minioWrapper.removeObject(getMinio(clusterId), backupName);
        } catch (Exception e) {
            log.error("删除备份失败", e);
        }
        return BaseResult.ok();
    }

    @Override
    public BaseResult createBackupSchedule(MiddlewareBackupDTO backupDTO) {
        // 校验是否运行中
        Middleware middleware = convertBackupToMiddleware(backupDTO);
        middlewareCRService.getCRAndCheckRunning(middleware);

        Minio minio = getMinio(backupDTO.getClusterId());
        BackupTemplate backupTemplate = new BackupTemplate().setClusterName(backupDTO.getMiddlewareName())
                .setStorageProvider(new BackupStorageProvider().setMinio(minio));

        MysqlScheduleBackupSpec spec =
                new MysqlScheduleBackupSpec().setSchedule(CronUtils.parseMysqlUtcCron(backupDTO.getCron())).setBackupTemplate(backupTemplate).setKeepBackups(backupDTO.getLimitRecord());
        ObjectMeta metaData = new ObjectMeta();
        metaData.setName(backupDTO.getMiddlewareName());
        Map<String, String> labels = new HashMap<>();
        labels.put("controllername", "backup-schedule-controller");
        metaData.setLabels(labels);
        metaData.setNamespace(backupDTO.getNamespace());
        metaData.setClusterName(backupDTO.getMiddlewareName());

        MysqlScheduleBackupCR mysqlScheduleBackupCR =
                new MysqlScheduleBackupCR().setKind("MysqlBackupSchedule").setSpec(spec).setMetadata(metaData);
        mysqlScheduleBackupService.create(backupDTO.getClusterId(), mysqlScheduleBackupCR);
        return BaseResult.ok();
    }

    @Override
    public BaseResult createNormalBackup(MiddlewareBackupDTO backupDTO) {
        middlewareCRService.getCRAndCheckRunning(convertBackupToMiddleware(backupDTO));
        BackupSpec spec = new BackupSpec().setClusterName(backupDTO.getMiddlewareName())
                .setStorageProvider(new BackupStorageProvider().setMinio(getMinio(backupDTO.getClusterId())));
        ObjectMeta metaData = new ObjectMeta();
        metaData.setName(backupDTO.getMiddlewareName() + UUIDUtils.get8UUID());
        Map<String, String> labels = new HashMap<>(1);
        labels.put("controllername", "backup-controller");
        metaData.setLabels(labels);
        metaData.setNamespace(backupDTO.getNamespace());
        metaData.setClusterName(backupDTO.getMiddlewareName());
        BackupCR backupCR = new BackupCR().setKind("MysqlBackup").setSpec(spec).setMetadata(metaData);
        backupService.create(backupDTO.getClusterId(), backupCR);
        return BaseResult.ok();
    }

    @Override
    public BaseResult createRestore(String clusterId, String namespace, String middlewareName, String type, String backupName, String backupFileName, List<String> pods) {
        Middleware middleware = middlewareService.detail(clusterId, namespace, middlewareName, type);
        middleware.setChartName(type);
        fixStorageUnit(middleware);
        middleware.setClusterId(clusterId);
        MiddlewareQuota mysql = middleware.getQuota().get("mysql");
        String storageClassQuota = mysql.getStorageClassQuota();
        mysql.setStorageClassQuota(String.valueOf(Integer.parseInt(storageClassQuota) + 3));
        middleware.setBackupFileName(backupFileName);
        middleware.setDeleteBackupInfo(false);
        // 设置不删除平台存储的数据库管理相关数据
        MysqlDTO mysqlDTO = new MysqlDTO();
        mysqlDTO.setDeleteDBManageInfo(false);
        middleware.setMysqlDTO(mysqlDTO);
        // 删除原中间件
        BaseOperator operator = middlewareService.getOperator(BaseOperator.class, BaseOperator.class, middleware);
        operator.delete(middleware);
        operator.deleteStorage(middleware);

        tryCreateMiddleware(clusterId, namespace, type, middlewareName, middleware);
        return BaseResult.ok();
    }

    @Override
    public void tryCreateMiddlewareRestore(String clusterId, String namespace, String type, String middlewareName, String backupName, String restoreName) {

    }

    @Override
    public void deleteMiddlewareBackupInfo(String clusterId, String namespace, String type, String middlewareName) {
        List<Backup> backupList = backupService.listBackup(clusterId, namespace);
        backupList.forEach(backup -> {
            if (!backup.getName().contains(middlewareName)) {
                return;
            }
            try {
                deleteRecord(clusterId, namespace, middlewareName, type, backup.getName(), backup.getBackupFileName());
            } catch (Exception e) {
                log.error("集群：{}，命名空间：{}，mysql中间件：{}，删除mysql备份异常", e);
            }
        });
    }

    @Override
    public List<MiddlewareBackupScheduleConfig> listBackupSchedule(String clusterId, String namespace, String type, String middlewareName, String keyword) {
        List<ScheduleBackup> scheduleBackupList = mysqlScheduleBackupService.listScheduleBackup(clusterId, namespace, middlewareName);
        List<MiddlewareBackupScheduleConfig> scheduleConfigList = new ArrayList<>();
        if (CollectionUtils.isEmpty(scheduleBackupList)) {
            return scheduleConfigList;
        }
        ScheduleBackup scheduleBackup = scheduleBackupList.get(0);
        MiddlewareBackupScheduleConfig config = new MiddlewareBackupScheduleConfig();
        config.setBackupScheduleName(scheduleBackup.getName());
        config.setCron(CronUtils.parseLocalCron(scheduleBackup.getSchedule()));
        config.setLimitRecord(scheduleBackup.getKeepBackups());
        if(null != scheduleBackup.getLastBackupTime()){
            String lastBackupTime = DateUtil.utc2Local(scheduleBackup.getLastBackupTime(), DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
            config.setCreateTime(lastBackupTime);
        }
        config.setCanPause(false);
        config.setPause("off");
        config.setSourceName(middlewareName);
        config.setBackupType(BackupType.CLUSTER.getType());
        scheduleConfigList.add(config);
        if (StringUtils.isNotBlank(keyword)) {
            return scheduleConfigList.stream().filter(record -> {
                if (record.getSourceName().contains(keyword)) {
                    return true;
                } else {
                    return false;
                }
            }).collect(Collectors.toList());
        }
        return scheduleConfigList;
    }

    @Override
    public BaseResult deleteSchedule(String clusterId, String namespace, String type, String backupScheduleName) {
        mysqlScheduleBackupService.delete(clusterId, namespace, backupScheduleName);
        return BaseResult.ok();
    }

    @Override
    public boolean checkIfAlreadyBackup(String clusterId, String namespace, String type, String middlewareName) {
        List<ScheduleBackup> scheduleBackupList = mysqlScheduleBackupService.listScheduleBackup(clusterId, namespace, middlewareName);
        if (scheduleBackupList != null && scheduleBackupList.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean checkIfAlreadyBackup(String clusterId, String namespace, String type, String middlewareName, String podName) {
        return false;
    }

    private void tryCreateMiddleware(String clusterId, String namespace, String type, String middlewareName, Middleware middleware) {
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

    public Minio getMinio(String clusterId) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        // 获取minio的数据
        Object backupObj = cluster.getStorage().getBackup();
        if (backupObj == null) {
            throw new BusinessException(ErrorMessage.MIDDLEWARE_BACKUP_STORAGE_NOT_EXIST);
        }
        JSONObject backup = JSONObject.parseObject(JSONObject.toJSONString(backupObj));
        return JSONObject.toJavaObject(backup.getJSONObject(STORAGE), Minio.class);
    }

    /**
     * 将备份信息转为middleware对象
     *
     * @param backupDTO
     * @return
     */
    private Middleware convertBackupToMiddleware(MiddlewareBackupDTO backupDTO) {
        return new Middleware().setClusterId(backupDTO.getClusterId())
                .setNamespace(backupDTO.getNamespace())
                .setType(backupDTO.getType())
                .setName(backupDTO.getMiddlewareName());
    }

    /**
     * 查询备份记录列表
     *
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @return
     */
    private List<MysqlBackupDto> listRecord(String clusterId, String namespace, String middlewareName) {
        List<Backup> backupList = backupService.listBackup(clusterId, namespace);
        backupList = backupList.stream().filter(backup -> backup.getName().contains(middlewareName)).collect(Collectors.toList());
        List<MysqlBackupDto> mysqlBackupDtoList = new ArrayList<>();
        // 设置备份状态
        backupList.forEach(backup -> {
            MysqlBackupDto mysqlBackupDto = new MysqlBackupDto();
            if (!"Complete".equals(backup.getPhase())) {
                mysqlBackupDto.setStatus(backup.getPhase());
                mysqlBackupDto.setBackupFileName("");
            } else {
                mysqlBackupDto.setStatus("Complete");
                mysqlBackupDto.setBackupFileName(backup.getBackupFileName());
            }
            mysqlBackupDto.setBackupName(backup.getName());
            mysqlBackupDto.setDate(DateUtils.parseUTCDate(backup.getBackupTime()));
            mysqlBackupDto.setPosition("minio(" + backup.getEndPoint() + "/" + backup.getBucketName() + ")");
            mysqlBackupDto.setType("all");
            mysqlBackupDtoList.add(mysqlBackupDto);
        });
        // 根据时间降序
        mysqlBackupDtoList.sort(
                (o1, o2) -> o1.getDate() == null ? -1 : o2.getDate() == null ? -1 : o2.getDate().compareTo(o1.getDate()));
        return mysqlBackupDtoList;
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
                if (v.getStorageClassQuota() != null){
                    v.setStorageClassQuota(v.getStorageClassQuota().replaceAll("Gi", ""));
                }
            });
        }
    }

}
