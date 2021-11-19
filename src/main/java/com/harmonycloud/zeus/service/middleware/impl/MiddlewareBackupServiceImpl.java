package com.harmonycloud.zeus.service.middleware.impl;

import com.harmonycloud.caas.common.base.BaseResult;
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
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.util.CronUtils;
import com.harmonycloud.zeus.util.DateUtil;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.harmonycloud.caas.common.enums.ErrorMessage.BACKUP_ALREADY_EXISTS;

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
    private MiddlewareBackupCRDService backupCRDService;
    @Autowired
    private MiddlewareRestoreCRDService restoreCRDService;
    @Autowired
    private MiddlewareCRDService middlewareCRDService;
    @Autowired
    private MysqlBackupServiceImpl mysqlAdapterService;
    @Autowired
    private PodService podService;

    @Override
    public List<MiddlewareBackupRecord> listRecord(String clusterId, String namespace, String middlewareName, String type) {
        if ("mysql".equals(type)) {
            return mysqlAdapterService.listRecord(clusterId, namespace, middlewareName, type);
        }
        List<MiddlewareBackupRecord> recordList = new ArrayList<>();
        List<MiddlewareBackupCRD> backupRecordList = getBackupRecordList(clusterId, namespace, middlewareName, type);
        if (!CollectionUtils.isEmpty(backupRecordList)) {
            backupRecordList.forEach(item -> {
                MiddlewareBackupStatus backupStatus = item.getStatus();
                MiddlewareBackupRecord backupRecord = new MiddlewareBackupRecord();
                setBackupPodName(clusterId, namespace, middlewareName, type, item.getSpec().getBackupObjects(), backupRecord);
                String backupTime = DateUtil.utc2Local(item.getMetadata().getCreationTimestamp(), DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
                backupRecord.setBackupTime(backupTime);
                backupRecord.setBackupName(item.getMetadata().getName());
                if (backupStatus != null) {
                    MiddlewareBackupStatus.StorageProvider.Minio minio = item.getStatus().getStorageProvider().getMinio();
                    String backupAddressPrefix = minio.getUrl() + "/" + minio.getBucket() + "/" + (minio.getPrefix() != null ? minio.getPrefix() + "-" : "");
                    List<MiddlewareBackupStatus.BackupInfo> backupInfos = backupStatus.getBackupInfos();
                    if (backupInfos != null) {
                        List<String> backupAddressList = new ArrayList<>();
                        for (MiddlewareBackupStatus.BackupInfo backupInfo : backupInfos) {
                            if (!StringUtils.isBlank(backupInfo.getRepository())) {
                                backupAddressList.add(backupAddressPrefix + backupInfo.getRepository());
                                backupRecord.setBackupAddressList(backupAddressList);
                            }
                        }
                    }
                    backupRecord.setPhrase(item.getStatus().getPhase());
                }
                recordList.add(backupRecord);
            });
        }
        return recordList;
    }

    @Override
    public BaseResult createBackup(MiddlewareBackupDTO backupDTO) {
        if ("mysql".equals(backupDTO.getType())) {
            return mysqlAdapterService.createBackup(backupDTO);
        }
        convertMiddlewareBackup(backupDTO);
        if (StringUtils.isBlank(backupDTO.getCron())) {
            return createNormalBackup(backupDTO);
        } else {
            return createBackupSchedule(backupDTO);
        }
    }

    @Override
    public BaseResult updateBackupSchedule(MiddlewareBackupDTO backupDTO) {
        if ("mysql".equals(backupDTO.getType())) {
            return mysqlAdapterService.updateBackupSchedule(backupDTO);
        }
        convertMiddlewareBackup(backupDTO);
        MiddlewareBackupScheduleCRD middlewareBackupScheduleCRD = backupScheduleCRDService.get(backupDTO.getClusterId(),
                backupDTO.getNamespace(), backupDTO.getBackupScheduleName());
        try {
            MiddlewareBackupScheduleSpec spec = middlewareBackupScheduleCRD.getSpec();
            spec.getSchedule().setCron(CronUtils.parseUtcCron(backupDTO.getCron()));
            spec.getSchedule().setLimitRecord(backupDTO.getLimitRecord());
            if (StringUtils.isBlank(backupDTO.getPause())) {
                spec.setPause("off");
            } else {
                spec.setPause(backupDTO.getPause());
            }
            backupScheduleCRDService.update(backupDTO.getClusterId(), middlewareBackupScheduleCRD);
            return BaseResult.ok();
        } catch (IOException e) {
            log.error("中间件{}备份设置更新失败", backupDTO.getMiddlewareName());
            return BaseResult.error();
        }
    }

    @Override
    public BaseResult deleteRecord(String clusterId, String namespace, String middlewareName, String type, String backupName, String backupFileName) {
        try {
            if ("mysql".equals(type)) {
                return mysqlAdapterService.deleteRecord(clusterId, namespace, middlewareName, type, backupName, backupFileName);
            }
            backupCRDService.delete(clusterId, namespace, backupName);
            return BaseResult.ok();
        } catch (IOException e) {
            log.error("删除备份记录失败");
            return BaseResult.error();
        }
    }

    public BaseResult createBackupSchedule(MiddlewareBackupDTO backupDTO) {
        Map<String, String> labels = getMiddlewareBackupLabels(backupDTO.getMiddlewareRealName(), null, backupDTO.getPods());
        MiddlewareBackupScheduleList scheduleList = backupScheduleCRDService.list(backupDTO.getClusterId(), backupDTO.getNamespace(), labels);
        if (!CollectionUtils.isEmpty(scheduleList.getItems())) {
            if (CollectionUtils.isEmpty(backupDTO.getPods())) {
                log.warn("服务已备份");
            } else {
                log.warn("Pod已备份");
            }
            return BaseResult.error(BACKUP_ALREADY_EXISTS);
        }
        MiddlewareBackupScheduleCRD crd = new MiddlewareBackupScheduleCRD();
        List<BackupObject> backupObjects = convertMiddlewareBackupObject(backupDTO);
        addPodRoleLabel(backupDTO.getLabels(), backupObjects);
        ObjectMeta meta = getMiddlewareBackupMeta(backupDTO.getNamespace(), backupDTO.getMiddlewareRealName(), backupDTO.getLabels(), backupDTO.getPods());
        crd.setMetadata(meta);
        MiddlewareBackupScheduleSpec spec = new MiddlewareBackupScheduleSpec(backupDTO.getMiddlewareName(), backupDTO.getCrdType(),
                CronUtils.parseUtcCron(backupDTO.getCron()), backupDTO.getLimitRecord(), backupObjects);
        spec.setPause("off");
        crd.setSpec(spec);
        try {
            backupScheduleCRDService.create(backupDTO.getClusterId(), crd);
            return BaseResult.ok();
        } catch (IOException e) {
            log.error("备份创建失败", e);
            return BaseResult.error();
        }
    }

    public BaseResult createNormalBackup(MiddlewareBackupDTO backupDTO) {
        MiddlewareBackupCRD middlewareBackupCRD = new MiddlewareBackupCRD();
        List<BackupObject> backupObjects = convertMiddlewareBackupObject(backupDTO);
        ObjectMeta meta = getMiddlewareBackupMeta(backupDTO.getNamespace(), backupDTO.getMiddlewareRealName(), backupDTO.getLabels(), backupDTO.getPods());
        middlewareBackupCRD.setMetadata(meta);
        MiddlewareBackupSpec spec = new MiddlewareBackupSpec();
        spec.setName(backupDTO.getMiddlewareName());
        spec.setType(backupDTO.getCrdType());
        spec.setBackupObjects(backupObjects);
        middlewareBackupCRD.setSpec(spec);
        try {
            backupCRDService.create(backupDTO.getClusterId(), middlewareBackupCRD);
            return BaseResult.ok();
        } catch (IOException e) {
            log.error("立即备份失败", e);
            return BaseResult.error();
        }
    }

    /**
     * 获取中间件备份Meta
     *
     * @param namespace          命名空间
     * @param middlewareRealName 中间件名称
     * @return
     */
    public ObjectMeta getMiddlewareBackupMeta(String namespace, String middlewareRealName, Map<String, String> labels, List<String> pods) {
        ObjectMeta metaData = new ObjectMeta();
        metaData.setNamespace(namespace);
        metaData.setName(middlewareRealName + "-backup-" + UUIDUtils.get8UUID());
        metaData.setLabels(getMiddlewareBackupLabels(middlewareRealName, labels, pods));
        return metaData;
    }

    /**
     * 获取中间件备份label
     *
     * @param middlewareRealName
     * @param labels
     * @param pods
     * @return
     */
    public Map<String, String> getMiddlewareBackupLabels(String middlewareRealName, Map<String, String> labels, List<String> pods) {
        Map<String, String> backupLabel = getBackupLabel(middlewareRealName);
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

    public Map<String, String> getBackupLabel(String middlewareRealName) {
        Map<String, String> labels = new HashMap<>();
        labels.put("owner", middlewareRealName + "-backup");
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
        return MiddlewareTypeEnum.findByType(type).getMiddlewareCrdType() + "-" + middlewareName;
    }

    /**
     * 获取中间件恢复名称
     *
     * @param type
     * @param middlewareName
     * @return
     */
    public String getRestoreName(String type, String middlewareName) {
        return MiddlewareTypeEnum.findByType(type).getMiddlewareCrdType() + "-" + middlewareName + "-restore" + UUIDUtils.get8UUID();
    }

    @Override
    public BaseResult createRestore(String clusterId, String namespace, String middlewareName, String type, String backupName, String backupFileName, List<String> pods) {
        //设置中间件恢复信息
        try {
            if ("mysql".equals(type)) {
                mysqlAdapterService.createRestore(clusterId, namespace, middlewareName, type, backupName, backupFileName, pods);
                return BaseResult.ok();
            }
            MiddlewareCRD cr = middlewareCRDService.getCR(clusterId, namespace, type, middlewareName);
            createMiddlewareRestore(clusterId, namespace, type, middlewareName, backupName, cr.getStatus(), pods);
            return BaseResult.ok();
        } catch (Exception e) {
            log.error("备份服务创建失败", e);
            return BaseResult.error();
        }
    }

    @Deprecated
    public void tryCreateMiddlewareRestore(String clusterId, String namespace, String type, String middlewareName, String backupName, String restoreName) {
        for (int i = 0; i < 600; i++) {
            log.info("第 {} 次为实例：{}创建恢复实例:{}", i, middlewareName, restoreName);
            MiddlewareCRD cr = null;
            try {
                cr = middlewareCRDService.getCR(clusterId, namespace, type, restoreName);
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
        MiddlewareBackupScheduleList backupScheduleList = backupScheduleCRDService.list(clusterId, namespace, labels);
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

    @Override
    public List<MiddlewareBackupScheduleConfig> listBackupSchedule(String clusterId, String namespace, String type, String middlewareName) {
        if (type.equals("mysql")) {
            return mysqlAdapterService.listBackupSchedule(clusterId, namespace, type, middlewareName);
        }
        MiddlewareBackupScheduleList scheduleList = backupScheduleCRDService.list(clusterId, namespace, getBackupLabel(middlewareName, type));
        List<MiddlewareBackupScheduleConfig> configList = new ArrayList<>();
        if (scheduleList != null && !CollectionUtils.isEmpty(scheduleList.getItems())) {
            scheduleList.getItems().forEach(schedule -> {
                MiddlewareBackupScheduleSpec spec = schedule.getSpec();
                Map<String, String> labels = schedule.getMetadata().getLabels();
                String createTime = DateUtil.utc2Local(schedule.getMetadata().getCreationTimestamp(), DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
                String backupType = labels.get(BackupConstant.KEY_BACKUP_TYPE);
                if (backupType != null) {
                    String localCron = CronUtils.parseLocalCron(spec.getSchedule().getCron());
                    MiddlewareBackupScheduleConfig config = new MiddlewareBackupScheduleConfig(schedule.getMetadata().getName(), true, localCron,
                            spec.getSchedule().getLimitRecord(), spec.getPause(), getBackupSourceName(schedule, backupType), backupType, createTime, getPodRole(backupType, labels));
                    configList.add(config);
                }
            });
        }
        return configList;
    }

    @Override
    public BaseResult deleteSchedule(String clusterId, String namespace, String type, String backupScheduleName) {
        try {
            backupScheduleCRDService.delete(clusterId, namespace, backupScheduleName);
            return BaseResult.ok();
        } catch (IOException e) {
            log.error("备份规则删除失败；{}", backupScheduleName, e);
            return BaseResult.error();
        }
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
    public void createMiddlewareRestore(String clusterId, String namespace, String type, String middlewareName, String backupName, MiddlewareStatus status, List<String> pods) {
        try {
            List<MiddlewareInfo> podList = status.getInclude().get(MiddlewareConstant.PODS);
            List<MiddlewareInfo> pvcs = status.getInclude().get(MiddlewareConstant.PERSISTENT_VOLUME_CLAIMS);
            if (CollectionUtils.isEmpty(pods)) {
                pods = new ArrayList<>();
                for (MiddlewareInfo pod : podList) {
                    pods.add(pod.getName());
                }
            }
            MiddlewareBackupCRD backup = backupCRDService.get(clusterId, namespace, backupName);
            MiddlewareRestoreCRD crd = new MiddlewareRestoreCRD();
            ObjectMeta meta = new ObjectMeta();
            meta.setNamespace(namespace);
            meta.setName(getRestoreName(type, middlewareName));
            Map<String, String> backupLabel = getBackupLabel(middlewareName, type);
            Map<String, String> middlewareLabel = getBackupLabel(middlewareName, type);
            backupLabel.putAll(middlewareLabel);
            meta.setLabels(backupLabel);
            crd.setMetadata(meta);
            MiddlewareRestoreSpec spec = new MiddlewareRestoreSpec();
            spec.setName(middlewareName);
            spec.setBackupName(backupName);
            spec.setRestoreObjects(convertRestoreObjects(pods, backup.getStatus().getBackupInfos(), pvcs));
            spec.setType(MiddlewareTypeEnum.findByType(type).getMiddlewareCrdType());
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
        String middlewareCrdType = MiddlewareTypeEnum.findByType(type).getMiddlewareCrdType();
        Map<String, String> backupLabel = getBackupLabel(middlewareName, type);
        Map<String, String> middlewareLabel = getBackupLabel(middlewareName, type);
        backupLabel.putAll(middlewareLabel);

        backupDTO.setLabels(backupLabel);
        backupDTO.setMiddlewareRealName(middlewareRealName);
        backupDTO.setCrdType(middlewareCrdType);
    }

    /**
     * 根据备份类型获取资源名称
     *
     * @param schedule   备份规则cr
     * @param backupType 备份类型
     * @return
     */
    private String getBackupSourceName(MiddlewareBackupScheduleCRD schedule, String backupType) {
        if (BackupType.CLUSTER.getType().equals(backupType)) {
            return schedule.getSpec().getName();
        } else if (BackupType.POD.getType().equals(backupType)) {
            List<BackupObject> backupObjects = schedule.getSpec().getBackupObjects();
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
     * 查询所有备份记录
     *
     * @param clusterId      集群id
     * @param namespace      分区
     * @param middlewareName 中间件名称
     * @param type           中间件类型
     */
    private List<MiddlewareBackupCRD> getBackupRecordList(String clusterId, String namespace, String middlewareName, String type) {
        List<MiddlewareBackupCRD> resList = new ArrayList<>();
        // 查询出所有备份规则
        MiddlewareBackupScheduleList scheduleList = backupScheduleCRDService.list(clusterId, namespace, getBackupLabel(middlewareName, type));
        // 查询出所有备份规则创建的备份记录
        scheduleList.getItems().forEach(schedule -> {
            String scheduleName = schedule.getMetadata().getName();
            Map<String, String> labels = new HashMap<>();
            labels.put("owner", scheduleName);
            MiddlewareBackupList backupList = backupCRDService.list(clusterId, namespace, labels);
            if (backupList != null && !CollectionUtils.isEmpty(backupList.getItems())) {
                resList.addAll(backupList.getItems());
            }
        });
        // 查询所有即时备份创建的备份记录
        Map<String, String> labels = new HashMap<>();
        labels.put("middleware", getRealMiddlewareName(type, middlewareName));
        MiddlewareBackupList backupList = backupCRDService.list(clusterId, namespace, labels);
        if (backupList != null && !CollectionUtils.isEmpty(backupList.getItems())) {
            resList.addAll(backupList.getItems());
        }
        return resList;
    }

    /**
     * 设置备份源名称与备份类型
     *
     * @param clusterId      集群id
     * @param namespace      分区
     * @param middlewareName 中间件名称
     * @param type           中间件类型
     * @param backupObjects  备份对象
     * @param backupRecord   备份记录
     * @return
     */
    private void setBackupPodName(String clusterId, String namespace, String middlewareName, String type, List<BackupObject> backupObjects, MiddlewareBackupRecord backupRecord) {
        Middleware middleware = podService.list(clusterId, namespace, middlewareName, type);
        List<PodInfo> pods = middleware.getPods();
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
        } else {
            StringBuffer sbf = new StringBuffer();
            backupPodList.forEach(pod -> {
                sbf.append(pod).append(",");
            });
            backupRecord.setSourceName(sbf.substring(0, sbf.length() - 1));
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


}
