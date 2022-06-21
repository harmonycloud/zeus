package com.harmonycloud.zeus.service.middleware.impl;

import com.harmonycloud.caas.common.constants.BackupConstant;
import com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant;
import com.harmonycloud.caas.common.enums.BackupType;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.MiddlewareBackupDTO;
import com.harmonycloud.caas.common.model.MiddlewareBackupScheduleConfig;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareBackupRecord;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.annotation.MiddlewareBackup;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.middleware.*;
import com.harmonycloud.zeus.util.CronUtils;
import com.harmonycloud.zeus.util.DateUtil;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 中间件通用备份
 *
 * @author liyinlong
 * @since 2021/9/15 3:22 下午
 */
@Slf4j
@MiddlewareBackup
@Service
public class MiddlewareBackupServiceImpl implements MiddlewareBackupService {

    @Autowired
    private MiddlewareBackupScheduleCRDService backupScheduleCRDService;
    @Autowired
    private MiddlewareBackupCRService backupCRDService;
    @Autowired
    private MiddlewareRestoreCRDService restoreCRDService;
    @Autowired
    private MiddlewareCRService middlewareCRService;
    @Autowired
    private MysqlBackupServiceImpl mysqlAdapterService;
    @Autowired
    private PodService podService;
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private MiddlewareCrTypeService middlewareCrTypeService;
    @Autowired
    private MiddlewareBackupAddressService middlewareBackupAddressService;

    @Override
    public List<MiddlewareBackupRecord> listRecord(String clusterId, String namespace, String middlewareName, String type, String keyword) {
        List<MiddlewareBackupRecord> recordList = new ArrayList<>();
        if ("mysql".equals(type)) {
            return mysqlAdapterService.listRecord(clusterId, namespace, middlewareName, type, keyword);
        }
        if (StringUtils.isEmpty(type)) {
            recordList.addAll(mysqlAdapterService.listRecord(clusterId, namespace, middlewareName, type, keyword));
        }
        List<MiddlewareBackupCR> backupRecordList = getBackupRecordList(clusterId, namespace, middlewareName, type);
        if (!CollectionUtils.isEmpty(backupRecordList)) {
            backupRecordList.forEach(item -> {
                MiddlewareBackupStatus backupStatus = item.getStatus();
                MiddlewareBackupRecord backupRecord = new MiddlewareBackupRecord();
                String backupTime = DateUtil.utc2Local(item.getMetadata().getCreationTimestamp(), DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
                backupRecord.setBackupTime(backupTime);
                backupRecord.setBackupName(item.getMetadata().getName());
                MiddlewareBackupSpec.MiddlewareBackupDestination.MiddlewareBackupParameters parameters =  item.getSpec().getBackupDestination().getParameters();
                String position = item.getSpec().getBackupDestination().getDestinationType() + "(" + parameters.getUrl() + "/" + parameters.getBucket() + ")";
                backupRecord.setPosition(position);
//                backupRecord.setSourceName();
                backupRecord.setPhrase(backupStatus.getPhase());
                backupRecord.setSourceType(item.getMetadata().getAnnotations().get("type"));
                backupRecord.setTaskName(item.getMetadata().getAnnotations().get("taskName"));
                backupRecord.setSourceName(item.getSpec().getName());
                backupRecord.setCron(null);
                recordList.add(backupRecord);
            });
        }
        if (StringUtils.isNotBlank(keyword)) {
            return recordList.stream().filter(record -> {
                if (record.getTaskName().contains(keyword)) {
                    return true;
                } else {
                    return false;
                }
            }).collect(Collectors.toList());
        }
        return recordList;
    }

    @Override
    public List<MiddlewareBackupRecord> listMysqlBackupScheduleRecord(String clusterId, String namespace, String backupName) {
        return null;
    }

    public List<MiddlewareBackupRecord> listBackupScheduleRecord(String clusterId, String namespace, String backupName, String type, String keyword) {
             List<MiddlewareBackupRecord> recordList = new ArrayList<>();
        List<MiddlewareBackupCR> backupRecordList = getScheduleBackupRecordList(clusterId, namespace);
        if (!CollectionUtils.isEmpty(backupRecordList)) {
            backupRecordList.forEach(item -> {
                MiddlewareBackupStatus backupStatus = item.getStatus();
                MiddlewareBackupRecord backupRecord = new MiddlewareBackupRecord();
//                setBackupSourceInfo(middlewareName, item.getSpec().getBackupObjects(), backupRecord, podInfo);
                String backupTime = DateUtil.utc2Local(item.getMetadata().getCreationTimestamp(), DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
                backupRecord.setBackupTime(backupTime);
                backupRecord.setBackupName(item.getMetadata().getName());
                MiddlewareBackupSpec spec = item.getSpec();
                MiddlewareBackupSpec.MiddlewareBackupDestination.MiddlewareBackupParameters parameters =  item.getSpec().getBackupDestination().getParameters();
                String position = item.getSpec().getBackupDestination().getDestinationType() + "(" + parameters.getUrl() + "/" + parameters.getBucket() + ")";
                backupRecord.setPosition(position);
//                backupRecord.setCron(item.getSpec().get);
                backupRecord.setPhrase(backupStatus.getPhase());
                backupRecord.setSourceName(item.getSpec().getName());
                backupRecord.setSourceType(item.getMetadata().getAnnotations().get("type"));
                backupRecord.setTaskName(item.getMetadata().getAnnotations().get("taskName"));
                backupRecord.setAddressName(item.getMetadata().getAnnotations().get("addressName"));
                recordList.add(backupRecord);
            });
        }
        if (StringUtils.isNotBlank(keyword)) {
            return recordList.stream().filter(record -> {
                if (record.getTaskName().contains(keyword)) {
                    return true;
                } else {
                    return false;
                }
            }).collect(Collectors.toList());
        }
        return recordList;
    }

    @Override
    public void createBackup(MiddlewareBackupDTO backupDTO) {
        middlewareCRService.getCRAndCheckRunning(convertBackupToMiddleware(backupDTO));
        if (MiddlewareTypeEnum.MYSQL.getType().equals(backupDTO.getType())) {
            mysqlAdapterService.createBackup(backupDTO);
        } else {
            convertMiddlewareBackup(backupDTO);
            if (StringUtils.isBlank(backupDTO.getCron())) {
                createNormalBackup(backupDTO);
            } else {
                createBackupSchedule(backupDTO);
            }
        }
    }

    @Override
    public void updateBackupSchedule(MiddlewareBackupDTO backupDTO) {
        if ("mysql".equals(backupDTO.getType())) {
            mysqlAdapterService.updateBackupSchedule(backupDTO);
        }
        convertMiddlewareBackup(backupDTO);
        MiddlewareBackupScheduleCR middlewareBackupScheduleCR = backupScheduleCRDService.get(backupDTO.getClusterId(),
                backupDTO.getNamespace(), backupDTO.getBackupScheduleName());
        try {
            MiddlewareBackupScheduleSpec spec = middlewareBackupScheduleCR.getSpec();
            spec.getSchedule().setCron(CronUtils.parseUtcCron(backupDTO.getCron()));
            spec.getSchedule().setLimitRecord(backupDTO.getLimitRecord());
            if (StringUtils.isBlank(backupDTO.getPause())) {
                spec.setPause("off");
            } else {
                spec.setPause(backupDTO.getPause());
            }
            backupScheduleCRDService.update(backupDTO.getClusterId(), middlewareBackupScheduleCR);
        } catch (IOException e) {
            log.error("中间件{}备份设置更新失败", backupDTO.getMiddlewareName());
        }
    }

    @Override
    public void deleteRecord(String clusterId, String namespace, String middlewareName, String type, String backupName, String backupFileName, String addressName) {
        try {
            if ("mysql".equals(type)) {
                mysqlAdapterService.deleteRecord(clusterId, namespace, middlewareName, type, backupName, backupFileName, addressName);
            } else {
                backupCRDService.delete(clusterId, namespace, backupName);
            }
            middlewareBackupAddressService.calRelevanceNum(addressName, false);
        } catch (IOException e) {
            log.error("删除备份记录失败");
        }
    }

    /**
     * 创建通用备份(定时/周期)
     * @param backupDTO
     * @return
     */
    public void createBackupSchedule(MiddlewareBackupDTO backupDTO) {
        Map<String, String> labels =
            getMiddlewareBackupLabels(backupDTO.getMiddlewareName(), null, backupDTO.getPods());
        Minio minio = mysqlAdapterService.getMinio(backupDTO.getAddressName());
        MiddlewareBackupScheduleCR crd = new MiddlewareBackupScheduleCR();
        ObjectMeta meta = getMiddlewareBackupMeta(backupDTO.getNamespace(), backupDTO.getMiddlewareName(),
            backupDTO.getLabels(), backupDTO.getAnnotations(), backupDTO.getPods());
        crd.setMetadata(meta);
        MiddlewareBackupScheduleSpec.MiddlewareBackupScheduleDestination destination =
            new MiddlewareBackupScheduleSpec.MiddlewareBackupScheduleDestination();
        destination.setDestinationType("minio").setParameters(
            new MiddlewareBackupScheduleSpec.MiddlewareBackupScheduleDestination.MiddlewareBackupParameters(
                minio.getBucketName(), minio.getEndpoint(), "/" + backupDTO.getType(), minio.getAccessKeyId(), minio.getSecretAccessKey(),
                null));
        List<String> args = new ArrayList();
        args.add("--backupSize=10");
        List<Map<String, List<String>>> customBackups = new ArrayList<>();
        Map<String, List<String>> map = new HashMap<>();
        map.put("args",args);
        customBackups.add(map);
        MiddlewareBackupScheduleSpec.Schedule schedule = new MiddlewareBackupScheduleSpec.Schedule();
        schedule.setCron(CronUtils.parseUtcCron(backupDTO.getCron())).setLimitRecord(backupDTO.getLimitRecord());
        MiddlewareBackupScheduleSpec spec = new MiddlewareBackupScheduleSpec(destination, customBackups, backupDTO.getMiddlewareName(),
            backupDTO.getCrdType(), "off", CronUtils.parseUtcCron(backupDTO.getCron()), backupDTO.getLimitRecord());
        crd.setSpec(spec);
        try {
            backupScheduleCRDService.create(backupDTO.getClusterId(), crd);
            middlewareBackupAddressService.calRelevanceNum(backupDTO.getAddressName(), true);
        } catch (IOException e) {
            log.error("备份创建失败", e);
        }
    }

    /**
     * 创建通用备份
     * @param backupDTO
     */
    public void createNormalBackup(MiddlewareBackupDTO backupDTO) {
        MiddlewareBackupCR middlewareBackupCR = new MiddlewareBackupCR();
        ObjectMeta meta = getMiddlewareBackupMeta(backupDTO.getNamespace(), backupDTO.getMiddlewareName(),
            backupDTO.getLabels(), backupDTO.getAnnotations(), backupDTO.getPods());
        middlewareBackupCR.setMetadata(meta);
        Minio minio = mysqlAdapterService.getMinio(backupDTO.getAddressName());
        MiddlewareBackupSpec.MiddlewareBackupDestination destination = new MiddlewareBackupSpec.MiddlewareBackupDestination();
        destination.setDestinationType("minio").setParameters(new MiddlewareBackupSpec.MiddlewareBackupDestination.MiddlewareBackupParameters(minio.getBucketName(),
                minio.getEndpoint(), "/" + backupDTO.getType(), minio.getAccessKeyId(), minio.getSecretAccessKey(), null));
        List<String> args = new ArrayList();
        args.add("--backupSize=10");
        List<Map<String, List<String>>> customBackups = new ArrayList<>();
        Map<String, List<String>> map = new HashMap<>();
        map.put("args",args);
        customBackups.add(map);
        MiddlewareBackupSpec spec = new MiddlewareBackupSpec(destination, backupDTO.getMiddlewareName(), backupDTO.getCrdType(), customBackups);
        middlewareBackupCR.setSpec(spec);
        try {
            backupCRDService.create(backupDTO.getClusterId(), middlewareBackupCR);
            middlewareBackupAddressService.calRelevanceNum(backupDTO.getAddressName(), true);
        } catch (IOException e) {
            log.error("立即备份失败", e);
        }
    }

    /**
     * 获取中间件备份Meta
     *
     * @param middlewareName 服务名称
     * @param namespace          命名空间
     * @return
     */
    public ObjectMeta getMiddlewareBackupMeta(String namespace, String middlewareName, Map<String, String> labels, Map<String, String> annotations, List<String> pods) {
        ObjectMeta metaData = new ObjectMeta();
        metaData.setNamespace(namespace);
        metaData.setName(middlewareName + "-" + UUIDUtils.get8UUID());
        metaData.setLabels(getMiddlewareBackupLabels(middlewareName, labels, pods));
        metaData.setAnnotations(annotations);
        return metaData;
    }

    /**
     * 获取中间件备份label
     *
     * @param middlewareName
     * @param labels
     * @param pods
     * @return
     */
    public Map<String, String> getMiddlewareBackupLabels(String middlewareName, Map<String, String> labels, List<String> pods) {
        Map<String, String> backupLabel = getBackupLabel(middlewareName);
        if (labels != null) {
            backupLabel.putAll(labels);
        }
        if (!CollectionUtils.isEmpty(pods)) {
            backupLabel.put(BackupConstant.KEY_BACKUP_TYPE, BackupType.POD.getType());
            for (String pod : pods) {
                backupLabel.put(pod, BackupConstant.ALREADY_BACKUP);
            }
        } else {
            backupLabel.put(BackupConstant.KEY_BACKUP_TYPE, BackupType.CLUSTER.getType());
        }
        return backupLabel;
    }

    /***
     * 转换备份信息
     * @param backupDTO
     * @return
     */
    public List<BackupObject> convertMiddlewareBackupObject(MiddlewareBackupDTO backupDTO) {
        Middleware middleware = podService.list(backupDTO.getClusterId(), backupDTO.getNamespace(), backupDTO.getMiddlewareName(), backupDTO.getType());
        List<String> podList = backupDTO.getPods();
        if (middleware == null) {
            return null;
        }
        List<BackupObject> backupObjects = new ArrayList<>();
        List<PodInfo> pods = middleware.getPods();
        pods.forEach(podInfo -> {
            if (CollectionUtils.isEmpty(podList)) {
                List<String> pvcs = podInfo.getPvcs();
                if (!CollectionUtils.isEmpty(pvcs)) {
                    BackupObject backupObject = new BackupObject(podInfo.getPodName(), pvcs);
                    backupObjects.add(backupObject);
                }
            } else {
                if (podList.contains(podInfo.getPodName())) {
                    List<String> pvcs = podInfo.getPvcs();
                    if (!CollectionUtils.isEmpty(pvcs)) {
                        BackupObject backupObject = new BackupObject(podInfo.getPodName(), podInfo.getRole(), pvcs);
                        backupObjects.add(backupObject);
                    }
                }
            }
        });
        return backupObjects;
    }

    /**
     * @param middlewareName
     * @param type
     * @return
     */
    public Map<String, String> getBackupLabel(String middlewareName, String type) {
        String middlewareRealName = getRealMiddlewareName(type, middlewareName);
        Map<String, String> labels = new HashMap<>();
        labels.put("middleware", middlewareRealName);
        return labels;
    }

    public Map<String, String> getBackupLabel(String middlewareName) {
        Map<String, String> labels = new HashMap<>();
        labels.put("owner", middlewareName + "-backup");
        return labels;
    }

    /**
     * 获取服务中间件名称
     *
     * @param type
     * @param middlewareName
     * @return
     */
    public String getRealMiddlewareName(String type, String middlewareName) {
        return middlewareCrTypeService.findByType(type) + "-" + middlewareName;
    }

    /**
     * 获取中间件恢复名称
     *
     * @param type
     * @param middlewareName
     * @return
     */
    public String getRestoreName(String type, String middlewareName) {
        return middlewareCrTypeService.findByType(type) + "-" + middlewareName + "-restore" + UUIDUtils.get8UUID();
    }

    @Override
    public void createRestore(String clusterId, String namespace, String middlewareName, String type, String backupName, String backupFileName, List<String> pods, String addressName) {
        //设置中间件恢复信息
        try {
            if ("mysql".equals(type)) {
                mysqlAdapterService.createRestore(clusterId, namespace, middlewareName, type, backupName, backupFileName, pods, addressName);
            }
            MiddlewareCR cr = middlewareCRService.getCR(clusterId, namespace, type, middlewareName);
            createMiddlewareRestore(clusterId, namespace, type, middlewareName, backupName, cr.getStatus(), pods, addressName);
        } catch (Exception e) {
            log.error("备份服务创建失败", e);
        }
    }

    @Deprecated
    public void tryCreateMiddlewareRestore(String clusterId, String namespace, String type, String middlewareName, String backupName, String restoreName) {
        for (int i = 0; i < 600; i++) {
            log.info("第 {} 次为实例：{}创建恢复实例:{}", i, middlewareName, restoreName);
            MiddlewareCR cr = null;
            try {
                cr = middlewareCRService.getCR(clusterId, namespace, type, restoreName);
            } catch (Exception e1) {
                try {
                    Thread.sleep(1000);
                    continue;
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
            }
            MiddlewareStatus status = cr.getStatus();
            if (status != null && "Running".equals(status.getPhase())) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e2) {
                e2.printStackTrace();
            }
        }
    }

    @Override
    public void deleteMiddlewareBackupInfo(String clusterId, String namespace, String type, String middlewareName) {
        Map<String, String> labels = getBackupLabel(middlewareName, type);
        //删除定时备份
        MiddlewareBackupScheduleList backupScheduleList = backupScheduleCRDService.list(clusterId, namespace);
        if (backupScheduleList != null && !CollectionUtils.isEmpty(backupScheduleList.getItems())) {
            backupScheduleList.getItems().forEach(item -> {
                try {
                    backupScheduleCRDService.delete(clusterId, namespace, item.getMetadata().getName());
                } catch (IOException e) {
                    log.error("删除定时备份失败");
                }
            });
        }
        //删除立即备份
        MiddlewareBackupList backupList = backupCRDService.list(clusterId, namespace, labels);
        if (backupList != null && !CollectionUtils.isEmpty(backupList.getItems())) {
            backupList.getItems().forEach(item -> {
                try {
                    backupCRDService.delete(clusterId, namespace, item.getMetadata().getName());
                } catch (IOException e) {
                    log.error("删除立即备份失败");
                }
            });
        }
        //删除恢复
        MiddlewareRestoreList restoreList = restoreCRDService.list(clusterId, namespace, labels);
        if (restoreList != null && !CollectionUtils.isEmpty(restoreList.getItems())) {
            restoreList.getItems().forEach(item -> {
                try {
                    restoreCRDService.delete(clusterId, namespace, item.getMetadata().getName());
                } catch (IOException e) {
                    log.error("删除恢复失败");
                }
            });
        }
    }

    /**
     * 定时备份任务
     */
    @Override
    public List<MiddlewareBackupRecord> listBackupSchedule(String clusterId, String namespace, String type, String middlewareName, String keyword) {
        List<MiddlewareBackupRecord> recordList = new ArrayList<>();
        if ("mysql".equals(type)) {
            return mysqlAdapterService.listBackupSchedule(clusterId, namespace, type, middlewareName, keyword);
        }
        if (StringUtils.isEmpty(type)) {
            recordList.addAll(mysqlAdapterService.listBackupSchedule(clusterId, namespace, type, middlewareName, keyword));
        }
        MiddlewareBackupScheduleList scheduleList = backupScheduleCRDService.list(clusterId, namespace);
//        Middleware middleware = middlewareService.detail(clusterId, namespace, middlewareName, type);
        if (scheduleList != null && !CollectionUtils.isEmpty(scheduleList.getItems())) {
            scheduleList.getItems().forEach(schedule -> {
                MiddlewareBackupScheduleStatus backupStatus = schedule.getStatus();
                MiddlewareBackupRecord backupRecord = new MiddlewareBackupRecord();
//                setBackupSourceInfo(middlewareName, item.getSpec().getBackupObjects(), backupRecord, podInfo);
                String backupTime = DateUtil.utc2Local(schedule.getMetadata().getCreationTimestamp(), DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
                backupRecord.setBackupTime(backupTime);
                backupRecord.setBackupName(schedule.getMetadata().getName());
                MiddlewareBackupScheduleSpec spec = schedule.getSpec();
                MiddlewareBackupScheduleSpec.MiddlewareBackupScheduleDestination.MiddlewareBackupParameters parameters =  spec.getBackupDestination().getParameters();
                String position = spec.getBackupDestination().getDestinationType() + "(" + parameters.getUrl() + "/" + parameters.getBucket() + ")";
                backupRecord.setPosition(position);
                backupRecord.setPhrase(backupStatus.getPhase());
                backupRecord.setSourceName(schedule.getSpec().getName());
                backupRecord.setSourceType(schedule.getMetadata().getAnnotations().get("type"));
//                setMiddlewareAliasName(middleware.getAliasName(), backupRecord);
                backupRecord.setTaskName(schedule.getMetadata().getAnnotations().get("taskName"));
                backupRecord.setAddressName(schedule.getMetadata().getAnnotations().get("addressName"));
                backupRecord.setCron(schedule.getSpec().getSchedule().getCron());
                recordList.add(backupRecord);
            });
        }
        if (StringUtils.isNotBlank(keyword)) {
            return recordList.stream().filter(record -> {
                if (record.getTaskName().contains(keyword)) {
                    return true;
                } else {
                    return false;
                }
            }).collect(Collectors.toList());
        }
        return recordList;
    }

    @Override
    public void deleteSchedule(String clusterId, String namespace, String type, String backupScheduleName, String addressName) {
        try {
            if ("mysql".equals(type)) {
                mysqlAdapterService.deleteSchedule(clusterId, namespace, type, backupScheduleName, addressName);
            }
            backupScheduleCRDService.delete(clusterId, namespace, backupScheduleName);
            middlewareBackupAddressService.calRelevanceNum(addressName, false);
        } catch (IOException e) {
            log.error("备份规则删除失败；{}", backupScheduleName, e);
        }
    }

    @Override
    public boolean checkIfAlreadyBackup(String clusterId, String namespace, String type, String middlewareName) {
        if ("mysql".equals(type)) {
            return mysqlAdapterService.checkIfAlreadyBackup(clusterId, namespace, type, middlewareName);
        }
        Map<String, String> labels = getMiddlewareBackupLabels(getRealMiddlewareName(type, middlewareName), null, null);
        MiddlewareBackupScheduleList scheduleList = backupScheduleCRDService.list(clusterId, namespace);
        if (scheduleList != null && !CollectionUtils.isEmpty(scheduleList.getItems())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean checkIfAlreadyBackup(String clusterId, String namespace, String type, String middlewareName, String podName) {
        if ("mysql".equals(type)) {
            return false;
        }
        List<String> pods = new ArrayList<>();
        if (StringUtils.isNotBlank(podName)) {
            pods.add(podName);
        }
        Map<String, String> labels = getMiddlewareBackupLabels(getRealMiddlewareName(type, middlewareName), null, pods);
        MiddlewareBackupScheduleList scheduleList = backupScheduleCRDService.list(clusterId, namespace);
        if (scheduleList != null && !CollectionUtils.isEmpty(scheduleList.getItems())) {
            return true;
        }
        return false;
    }

    /**
     * 查询所有的备份任务
     */
    @Override
    public List<MiddlewareBackupRecord> backupTaskList(String clusterId, String namespace, String middlewareName, String type, String keyword) {
        List<MiddlewareBackupRecord> recordList = new ArrayList<>();
        List<MiddlewareBackupRecord> backupRecords = listRecord(clusterId, namespace, middlewareName, type, keyword);
        List<MiddlewareBackupRecord> backupSchedules = listBackupSchedule(clusterId, namespace, type, middlewareName, keyword);
        //过滤backupSchedule所产生的backup
        backupSchedules.forEach(schedule -> {
            backupRecords.stream().filter(record -> record.getBackupName().equals(schedule.getBackupName())).collect(Collectors.toList());
        });
        recordList.addAll(backupRecords);
        recordList.addAll(backupSchedules);
         return recordList;
    }

    @Override
    public List<MiddlewareBackupRecord> backupRecords(String clusterId, String namespace, String backupName, String type) {
        List<MiddlewareBackupRecord> records;
        if (MiddlewareTypeEnum.MYSQL.getType().equals(type)) {
            records = mysqlAdapterService.listMysqlBackupScheduleRecord(clusterId, namespace, backupName);
        } else {
            records = listBackupScheduleRecord(clusterId, namespace, backupName, type, null);
        }
        return records.stream().filter(record -> backupName.equals(record.getBackupName())).collect(Collectors.toList());
    }

    @Override
    public void deleteBackUpTask(String clusterId, String namespace, String type, String backupName, String backupFileName, String addressName, String cron) {
        if (StringUtils.isEmpty(cron)) {
            deleteRecord(clusterId, namespace, null, type, backupName, backupFileName, addressName);
        } else {
            deleteSchedule(clusterId, namespace, type, backupName, addressName);
        }
    }

    @Override
    public void deleteBackUpRecord(String clusterId, String namespace, String type, String backupName, String backupFileName, String addressName) {
        deleteRecord(clusterId, namespace, null, type, backupName, backupFileName, addressName);
    }

    /**
     * 创建中间件恢复
     *
     * @param clusterId      集群id
     * @param namespace      分区
     * @param type           中间件类型
     * @param middlewareName 源中间件名称
     * @param backupName     备份名称
     * @param status         恢复中间件状态信息
     */
    public void createMiddlewareRestore(String clusterId, String namespace, String type, String middlewareName, String backupName, MiddlewareStatus status, List<String> pods, String addressName) {
        try {
            List<MiddlewareInfo> podList = status.getInclude().get(MiddlewareConstant.PODS);
            List<MiddlewareInfo> pvcs = status.getInclude().get(MiddlewareConstant.PERSISTENT_VOLUME_CLAIMS);
            if (CollectionUtils.isEmpty(pods)) {
                pods = new ArrayList<>();
                for (MiddlewareInfo pod : podList) {
                    pods.add(pod.getName());
                }
            }
            MiddlewareBackupCR backup = backupCRDService.get(clusterId, namespace, backupName);
            MiddlewareRestoreCR crd = new MiddlewareRestoreCR();
            Minio minio = mysqlAdapterService.getMinio(addressName);
            ObjectMeta meta = new ObjectMeta();
            meta.setNamespace(namespace);
            meta.setName(getRestoreName(type, middlewareName));
            Map<String, String> backupLabel = getBackupLabel(middlewareName, type);
            Map<String, String> middlewareLabel = getBackupLabel(middlewareName, type);
            backupLabel.putAll(middlewareLabel);
            meta.setLabels(backupLabel);
            crd.setMetadata(meta);
            MiddlewareRestoreSpec spec = new MiddlewareRestoreSpec();
            List<String> args = new ArrayList();
            args.add("--backupResultName=" + backupName);
            args.add("--backupNamespace=" + namespace);
            List<Map<String, List<String>>> customRestores = new ArrayList<>();
            Map<String, List<String>> map = new HashMap<>();
            map.put("args",args);
            customRestores.add(map);
            spec.setName(middlewareName);
            spec.setType(middlewareCrTypeService.findByType(type));
            spec.setCustomRestores(customRestores);
            crd.setSpec(spec);
            restoreCRDService.create(clusterId, crd);
        } catch (IOException e) {
            log.error("创建恢复实例出错了", e);
        }
    }

    /**
     * 转换备份信息
     *
     * @param backupDTO
     * @return
     */
    private void convertMiddlewareBackup(MiddlewareBackupDTO backupDTO) {
        String type = backupDTO.getType();
        String middlewareName = backupDTO.getMiddlewareName();

        String middlewareRealName = getRealMiddlewareName(type, middlewareName);
        String middlewareCrdType = middlewareCrTypeService.findByType(type);
        Map<String, String> backupLabel = getBackupLabel(middlewareName, type);
        Map<String, String> middlewareLabel = getBackupLabel(middlewareName, type);
        backupLabel.putAll(middlewareLabel);
        Map<String, String> annotations = new HashMap<>();
        annotations.put("taskName", backupDTO.getTaskName());
        annotations.put("addressName", backupDTO.getAddressName());
        annotations.put("type", backupDTO.getType());
        backupDTO.setLabels(backupLabel);
        backupDTO.setMiddlewareRealName(middlewareRealName);
        backupDTO.setCrdType(middlewareCrdType);
        backupDTO.setAnnotations(annotations);
    }

    /**
     * 根据备份类型获取资源名称
     *
     * @param schedule   备份规则cr
     * @param backupType 备份类型
     * @return
     */
    private String getBackupSourceName(MiddlewareBackupScheduleCR schedule, String backupType) {
        if (BackupType.CLUSTER.getType().equals(backupType)) {
            return schedule.getSpec().getName();
        } else if (BackupType.POD.getType().equals(backupType)) {
            List<BackupObject> backupObjects = new ArrayList<>();
//            List<BackupObject> backupObjects = schedule.getSpec().getBackupObjects();
            StringBuffer podNames = new StringBuffer();
            backupObjects.forEach(backupObject -> {
                podNames.append(backupObject.getPod()).append(",");
            });
            return podNames.substring(0, podNames.length() - 1);
        }
        return null;
    }

    /**
     * 获取pod角色
     *
     * @param backupType 备份类型
     * @param labels     备份记录标签
     * @return
     */
    private String getPodRole(String backupType, Map<String, String> labels) {
        if (BackupType.POD.getType().equals(backupType)) {
            return labels.get(BackupConstant.POD_ROLE);
        }
        return null;
    }

    /**
     * 如果是pod级别的备份，则给pod设置角色
     *
     * @param labels        备份规则label
     * @param backupObjects 备份对象信息
     */
    private void addPodRoleLabel(Map<String, String> labels, List<BackupObject> backupObjects) {
        BackupObject backupObject = backupObjects.get(0);
        String role = backupObject.getPodRole();
        if (StringUtils.isNotBlank(role)) {
            labels.put("podRole", role);
        }
    }

    /**
     * 查询备份记录(立即备份)
     */
    private List<MiddlewareBackupCR> getBackupRecordList(String clusterId, String namespace, String middlewareName, String type) {
        List<MiddlewareBackupCR> resList = new ArrayList<>();
        // 查询出所有备份规则
       /* MiddlewareBackupScheduleList scheduleList = backupScheduleCRDService.list(clusterId, namespace, getBackupLabel(middlewareName, type));
        // 查询出所有备份规则创建的备份记录
        scheduleList.getItems().forEach(schedule -> {
            String scheduleName = schedule.getMetadata().getName();
            Map<String, String> labels = new HashMap<>();
            labels.put("owner", scheduleName);
            MiddlewareBackupList backupList = backupCRDService.list(clusterId, namespace, labels);
            if (backupList != null && !CollectionUtils.isEmpty(backupList.getItems())) {
                resList.addAll(backupList.getItems());
            }
        });*/
        // 查询所有即时备份创建的备份记录
        MiddlewareBackupList backupList = new MiddlewareBackupList();
        if (StringUtils.isEmpty(middlewareName) && StringUtils.isEmpty(type)) {
            backupList = backupCRDService.list(clusterId, namespace);
        } else {
            Map<String, String> labels = new HashMap<>();
            labels.put("middleware", getRealMiddlewareName(type, middlewareName));
            backupList = backupCRDService.list(clusterId, namespace, labels);
        }
         if (backupList != null && !CollectionUtils.isEmpty(backupList.getItems())) {
            resList.addAll(backupList.getItems());
        }
        return resList;
    }

    private List<MiddlewareBackupCR> getScheduleBackupRecordList(String clusterId, String namespace) {
        List<MiddlewareBackupCR> resList = new ArrayList<>();
        MiddlewareBackupList backupList = backupCRDService.list(clusterId, namespace);
        if (backupList != null && !CollectionUtils.isEmpty(backupList.getItems())) {
            resList.addAll(backupList.getItems());
        }
        return resList;
    }

    /**
     * 设置备份源名称与备份类型
     *
     * @param middlewareName 中间件名称
     * @param backupObjects  备份对象
     * @param backupRecord   备份记录
     * @return
     */
    private void setBackupSourceInfo(String middlewareName, List<BackupObject> backupObjects, MiddlewareBackupRecord backupRecord, Middleware middleware) {
        List<PodInfo> pods = middleware.getPods().stream().filter(podInfo -> {
            if (podInfo.getResources() != null && podInfo.getResources().getIsLvmStorage() != null && podInfo.getResources().getIsLvmStorage()) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        List<String> podList = new ArrayList<>();
        pods.forEach(podInfo -> {
            podList.add(podInfo.getPodName());
        });
        List<String> backupPodList = new ArrayList<>();
        backupObjects.forEach(backupObject -> {
            backupPodList.add(backupObject.getPod());
        });
        if (podList.containsAll(backupPodList) && podList.size() == backupPodList.size()) {
            backupRecord.setSourceName(middlewareName);
            backupRecord.setBackupType(BackupType.CLUSTER.getType());
            backupRecord.setAliasName(middleware.getAliasName());
        } else {
            String podName = backupPodList.get(0);
            pods.forEach(pod -> {
                if (pod.getPodName().equals(podName)) {
                    backupRecord.setPodRole(pod.getRole());
                }
            });
            backupRecord.setSourceName(podName);
            backupRecord.setBackupType(BackupType.POD.getType());
        }
    }

    /**
     * 转换恢复对象
     *
     * @param pods        要恢复服务的pod列表
     * @param backupInfos 备份记录信息
     * @param pvcs        要恢复的服务的所有pvc
     * @return
     */
    private List<RestoreObject> convertRestoreObjects(List<String> pods, List<MiddlewareBackupStatus.BackupInfo> backupInfos, List<MiddlewareInfo> pvcs) {
        List<RestoreObject> restoreObjects = new ArrayList<>();
        pods.forEach(pod -> {
            RestoreObject restoreObject = new RestoreObject();
            restoreObject.setPod(pod);
            for (MiddlewareBackupStatus.BackupInfo backupInfo : backupInfos) {
                if (backupInfo.getVolumeSnapshot().contains(pod)) {
                    restoreObject.setVolumeSnapshot(backupInfo.getVolumeSnapshot());
                    break;
                }
            }
            for (MiddlewareInfo pvc : pvcs) {
                if (pvc.getName().contains(pod)) {
                    restoreObject.setPvc(pvc.getName());
                    break;
                }
            }
            restoreObjects.add(restoreObject);
        });
        return restoreObjects;
    }

    /**
     * 设置服务别名
     * @param clusterId
     * @param namespace
     * @param type
     * @param middlewareName
     * @param config
     */
    private void setMiddlewareAliasName(String clusterId, String namespace, String type, String middlewareName, MiddlewareBackupScheduleConfig config) {
        if (BackupType.CLUSTER.getType().equals(config.getBackupType())) {
            Middleware middleware = middlewareService.detail(clusterId, namespace, middlewareName, type);
            config.setAliasName(middleware.getAliasName());
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

    private Middleware convertBackupToMiddleware(MiddlewareBackupDTO backupDTO) {
        return new Middleware().setClusterId(backupDTO.getClusterId())
                .setNamespace(backupDTO.getNamespace())
                .setType(backupDTO.getType())
                .setName(backupDTO.getMiddlewareName());
    }
}
