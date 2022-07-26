package com.harmonycloud.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.enums.BackupType;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.MiddlewareBackupDTO;
import com.harmonycloud.caas.common.model.MiddlewareBackupScheduleConfig;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.bean.BeanMiddlewareBackupName;
import com.harmonycloud.zeus.dao.BeanMiddlewareBackupNameMapper;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.integration.minio.MinioWrapper;
import com.harmonycloud.zeus.operator.BaseOperator;
import com.harmonycloud.zeus.schedule.MiddlewareManageTask;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRService;
import com.harmonycloud.zeus.service.middleware.BackupService;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupAddressService;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupService;
import com.harmonycloud.zeus.service.middleware.MysqlScheduleBackupService;
import com.harmonycloud.zeus.util.CronUtils;
import com.harmonycloud.zeus.util.DateUtil;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.text.DecimalFormat;
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
    @Autowired
    private MiddlewareBackupAddressService middlewareBackupAddressService;
    @Autowired
    private BeanMiddlewareBackupNameMapper middlewareBackupNameMapper;

    @Override
    public List<MiddlewareBackupRecord> listRecord(String clusterId, String namespace, String middlewareName, String type, String keyword) {
        List<MysqlBackupDto> backups = new ArrayList<>();
        backups = listRecord(clusterId, namespace, middlewareName);
        return convertMysqlBackupDto(backups, clusterId, keyword);
    }

    @Override
    public List<MiddlewareBackupRecord> listMysqlBackupScheduleRecord(String clusterId, String namespace, String backupName) {
        List<MysqlBackupDto> backups = listMysqlBackupDto(clusterId, namespace, backupName);
        return convertMysqlBackupDto(backups, clusterId, null);
    }

    private List<MiddlewareBackupRecord> convertMysqlBackupDto(List<MysqlBackupDto> backups,String clusterId, String keyword) {
        List<MiddlewareBackupRecord> list = new ArrayList<>();
        for (MysqlBackupDto backup : backups) {
            MiddlewareBackupRecord record = new MiddlewareBackupRecord();
            record.setNamespace(backup.getNamespace());
            record.setSourceName(backup.getName());
            record.setBackupType(BackupType.CLUSTER.getType());
            record.setBackupName(backup.getBackupName());
            record.setBackupFileName(backup.getBackupFileName());
            record.setSourceType(MiddlewareTypeEnum.MYSQL.getType());
            record.setOwner(backup.getOwner());
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
            record.setPosition(backup.getPosition());
            record.setAddressName(backup.getAddressName());
            if (StringUtils.isEmpty(backup.getTaskName()) && StringUtils.isNotEmpty(backup.getBackupName())){
                try {
                    MysqlScheduleBackupCR msbCr = mysqlScheduleBackupService.get(clusterId, backup.getNamespace(), backup.getBackupName());
                    String taskName = msbCr.getMetadata().getLabels().get("backupId");
                    backup.setTaskName(StringUtils.isEmpty(taskName) ? msbCr.getMetadata().getName() : taskName);
                } catch (Exception e){
                    log.error("查询定时文件资源失败", e);
                    continue;
                }
            }
            record.setBackupId(backup.getTaskName());
            String backupName = getBackupName(clusterId, backup.getTaskName()).getBackupName();
            record.setTaskName(StringUtils.isEmpty(backupName) ? backup.getTaskName() : backupName);
            if (StringUtils.isEmpty(record.getTaskName())){
                continue;
            }
            record.setBackupMode("single");
            list.add(record);
        }
        //添加备份记录名称
        list.stream().collect(Collectors.groupingBy(MiddlewareBackupRecord::getTaskName)).forEach((k, v) -> {
            for (int i = 0; i < v.size(); i++) {
                v.get(i).setRecordName(v.get(i).getTaskName() + "-" + "记录" + (i + 1));
            }
        });
        if (StringUtils.isNotBlank(keyword)) {
            return list.stream()
                .filter(
                    record -> StringUtils.isNotEmpty(record.getTaskName()) && record.getTaskName().contains(keyword))
                .collect(Collectors.toList());
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
    public void updateBackupSchedule(MiddlewareBackupDTO backupDTO) {
        MysqlScheduleBackupCR backupCRD = mysqlScheduleBackupService.get(backupDTO.getClusterId(), backupDTO.getNamespace(), backupDTO.getBackupScheduleName());
        MysqlScheduleBackupSpec spec = backupCRD.getSpec();
        spec.setSchedule(CronUtils.parseUtcCron(backupDTO.getCron()));
        spec.setKeepBackups(backupDTO.getLimitRecord());
        mysqlScheduleBackupService.update(backupDTO.getClusterId(), backupCRD);
    }

    @Override
    public void deleteRecord(String clusterId, String namespace, String middlewareName, String type,
        String backupName, String backupFileName, String chineseName) {
        try {
            backupService.delete(clusterId, namespace, backupName);
            minioWrapper.removeObject(getMinio(chineseName), backupName);
        } catch (Exception e) {
            log.error("删除备份失败", e);
        }
    }

    /**
     * 创建mysql备份(定时/周期)
     * @param backupDTO
     */
    @Override
    public void createBackupSchedule(MiddlewareBackupDTO backupDTO) {
        // 校验是否运行中
        Middleware middleware = convertBackupToMiddleware(backupDTO);
        middlewareCRService.getCRAndCheckRunning(middleware);

        Minio minio = getMinio(backupDTO.getAddressName());
        BackupTemplate backupTemplate = new BackupTemplate().setClusterName(backupDTO.getMiddlewareName())
                .setStorageProvider(new BackupStorageProvider().setMinio(minio));

        MysqlScheduleBackupSpec spec =
                new MysqlScheduleBackupSpec().setSchedule(CronUtils.parseUtcCron(backupDTO.getCron())).setBackupTemplate(backupTemplate).setKeepBackups(backupDTO.getLimitRecord());
        ObjectMeta metaData = new ObjectMeta();
        metaData.setName(backupDTO.getMiddlewareName() + UUIDUtils.get8UUID());
        Map<String, String> labels = new HashMap<>();
        labels.put("controllername", "backup-schedule-controller");
        String backupId = UUIDUtils.get16UUID();
        labels.put("backupId", backupId);
        labels.put("addressId", backupDTO.getAddressName());
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
     * @param backupDTO
     */
    @Override
    public void createNormalBackup(MiddlewareBackupDTO backupDTO) {
        middlewareCRService.getCRAndCheckRunning(convertBackupToMiddleware(backupDTO));
        BackupSpec spec = new BackupSpec().setClusterName(backupDTO.getMiddlewareName())
                .setStorageProvider(new BackupStorageProvider().setMinio(getMinio(backupDTO.getAddressName())));
        ObjectMeta metaData = new ObjectMeta();
        metaData.setName(backupDTO.getMiddlewareName() + "-" + UUIDUtils.get8UUID());
        Map<String, String> labels = new HashMap<>(1);
        labels.put("controllername", "backup-controller");
        String backupId = UUIDUtils.get16UUID();
        labels.put("backupId", backupId);
        labels.put("addressId", backupDTO.getAddressName());
        labels.put("type", backupDTO.getType());
        metaData.setLabels(labels);
        metaData.setNamespace(backupDTO.getNamespace());
        metaData.setClusterName(backupDTO.getMiddlewareName());
        BackupCR backupCR = new BackupCR().setKind("MysqlBackup").setSpec(spec).setMetadata(metaData);
        backupService.create(backupDTO.getClusterId(), backupCR);
        createBackupName(backupDTO.getClusterId(), backupDTO.getTaskName(), backupId, "normal");
    }

    @Override
    public void createRestore(String clusterId, String namespace, String middlewareName, String type, String backupName, String backupFileName, List<String> pods, String addressName) {
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
                deleteRecord(clusterId, namespace, middlewareName, type, backup.getName(), backup.getBackupFileName(), backup.getAddressName());
            } catch (Exception e) {
                log.error("集群：{}，命名空间：{}，mysql中间件：{}，删除mysql备份异常", e);
            }
        });
    }

    @Override
    public List<MiddlewareBackupRecord> listBackupSchedule(String clusterId, String namespace, String type, String middlewareName, String keyword) {
        List<MiddlewareBackupRecord> recordList = mysqlScheduleBackupService.listScheduleBackupRecord(clusterId, namespace, middlewareName);
        if (CollectionUtils.isEmpty(recordList)) {
            return new ArrayList<MiddlewareBackupRecord>();
        }
        if (StringUtils.isNotBlank(keyword)) {
            return recordList.stream().filter(record -> record.getTaskName().contains(keyword)).collect(Collectors.toList());
        }
        return recordList;
    }

    @Override
    public void deleteSchedule(String clusterId, String namespace, String type, String backupScheduleName, String addressName) {
        mysqlScheduleBackupService.delete(clusterId, namespace, backupScheduleName);
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

    @Override
    public List<MiddlewareBackupRecord> backupTaskList(String clusterId, String namespace, String middlewareName, String type, String keyword) {
        return null;
    }

    @Override
    public List<MiddlewareBackupRecord> backupRecords(String clusterId, String namespace, String middlewareName, String type) {
        return null;
    }

    @Override
    public void deleteBackUpTask(String clusterId, String namespace, String type, String backupName, String backupId, String backupFileName, String addressName, String cron) {

    }

    @Override
    public void deleteBackUpRecord(String clusterId, String namespace, String type, String backupName, String backupFileName, String addressName, String backupId) {

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
    public MiddlewareBackupNameDTO getBackupName(String clusterId, String backupId) {
        QueryWrapper<BeanMiddlewareBackupName> wrapper = new QueryWrapper<BeanMiddlewareBackupName>().eq("cluster_id", clusterId).eq("backup_id", backupId);
        BeanMiddlewareBackupName backupName = middlewareBackupNameMapper.selectOne(wrapper);
        MiddlewareBackupNameDTO backupNameDTO = new MiddlewareBackupNameDTO();
        if (!ObjectUtils.isEmpty(backupName)) {
            BeanUtils.copyProperties(backupName, backupNameDTO);
        }
        return backupNameDTO;
    }

    @Override
    public void deleteBackupName(String clusterId, String taskName, String backupType) {

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

    public Minio getMinio(String addressId) {
        List<MiddlewareClusterBackupAddressDTO> backupAddressDTOS = middlewareBackupAddressService.listBackupAddress(addressId, null);
        Minio minio = new Minio();
        if (!CollectionUtils.isEmpty(backupAddressDTOS)) {
            BeanUtils.copyProperties(backupAddressDTOS.get(0), minio);
        }
        if (minio.getBucketName().indexOf("/") == 0){
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
            mysqlBackupDto.setNamespace(backup.getNamespace());
            mysqlBackupDto.setBackupFileName(backup.getBackupFileName());
            mysqlBackupDto.setName(backup.getName());
            mysqlBackupDto.setBackupName(backup.getBackupName());
            mysqlBackupDto.setDate(DateUtils.parseUTCDate(backup.getBackupTime()));
            mysqlBackupDto.setPosition("minio(" + backup.getEndPoint() + "/" + backup.getBucketName() + ")");
            mysqlBackupDto.setAddressName(backup.getAddressName());
            mysqlBackupDto.setType(backup.getType());
            mysqlBackupDto.setTaskName(backup.getTaskName());
            mysqlBackupDto.setOwner(backup.getOwner());
            mysqlBackupDtoList.add(mysqlBackupDto);
        });
        // 根据时间降序
        mysqlBackupDtoList.sort(
                (o1, o2) -> o1.getDate() == null ? -1 : o2.getDate() == null ? -1 : o2.getDate().compareTo(o1.getDate()));
        return mysqlBackupDtoList;
    }

    public List<MysqlBackupDto> listMysqlBackupRecord(String clusterId, String namespace, String backupName) {
        List<Backup> backups = backupService.listBackup(clusterId,namespace);
        backups.stream().filter(backup -> backupName.equals(backup.getName())).collect(Collectors.toList());
        return convertBackup(backups);
    }

    public List<MysqlBackupDto> listMysqlBackupDto(String clusterId, String namespace, String backupName) {
        List<Backup> backups = backupService.listScheduleBackup(clusterId, namespace);
        return convertBackup(backups);
    }

    private List<MysqlBackupDto> convertBackup(List<Backup> backups) {
        List<MysqlBackupDto> mysqlBackupDtoList = new ArrayList<>();
        backups.forEach(backup -> {
            MysqlBackupDto mysqlBackupDto = new MysqlBackupDto();
            if (!"Complete".equals(backup.getPhase())) {
                mysqlBackupDto.setStatus(backup.getPhase());
                mysqlBackupDto.setBackupFileName("");
            } else {
                mysqlBackupDto.setStatus("Complete");
                mysqlBackupDto.setBackupFileName(backup.getBackupFileName());
            }
            mysqlBackupDto.setBackupName(backup.getBackupName());
            mysqlBackupDto.setDate(DateUtils.parseUTCDate(backup.getBackupTime()));
            mysqlBackupDto.setPosition("minio(" + backup.getEndPoint() + "/" + backup.getBucketName() + ")");
            mysqlBackupDto.setAddressName(backup.getAddressName());
            mysqlBackupDto.setType("all");
            mysqlBackupDto.setTaskName(backup.getTaskName());
            mysqlBackupDto.setName(backup.getName());
            mysqlBackupDto.setNamespace(backup.getNamespace());
            mysqlBackupDto.setBackupFileName(backup.getBackupFileName());
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

    /**
     * 设置服务别名
     * @param aliasName
     * @param record
     */
    private void setMiddlewareAliasName(String aliasName, MiddlewareBackupRecord record) {
        if (BackupType.CLUSTER.getType().equals(record.getBackupType())) {
            record.setAliasName(aliasName);
        }
    }

}
