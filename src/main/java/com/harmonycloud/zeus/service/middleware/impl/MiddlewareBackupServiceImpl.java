package com.harmonycloud.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.harmonycloud.caas.common.constants.BackupConstant;
import com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant;
import com.harmonycloud.caas.common.enums.BackupType;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.MiddlewareBackupDTO;
import com.harmonycloud.caas.common.model.MiddlewareBackupScheduleConfig;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.annotation.MiddlewareBackup;
import com.harmonycloud.zeus.bean.BeanMiddlewareBackupName;
import com.harmonycloud.zeus.dao.BeanMiddlewareBackupNameMapper;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.middleware.*;
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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.NameConstant.OWNER;

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
    private BeanMiddlewareBackupNameMapper middlewareBackupNameMapper;

    @Override
    public List<MiddlewareBackupRecord> listRecord(String clusterId, String namespace, String middlewareName, String type, String keyword) {
        List<MiddlewareBackupRecord> recordList = new ArrayList<>();
        if (MiddlewareTypeEnum.MYSQL.getType().equals(type)) {
            return mysqlAdapterService.listRecord(clusterId, namespace, middlewareName, type, keyword);
        }
        if (StringUtils.isEmpty(type)) {
            recordList = mysqlAdapterService.listRecord(clusterId, namespace, middlewareName, type, keyword);
        }
        List<MiddlewareBackupCR> backupRecordList = getBackupRecordList(clusterId, namespace, middlewareName, type);
        if (!CollectionUtils.isEmpty(backupRecordList)) {
            for (MiddlewareBackupCR item : backupRecordList) {
                MiddlewareBackupStatus backupStatus = item.getStatus();
                MiddlewareBackupRecord backupRecord = new MiddlewareBackupRecord();
                if (item.getMetadata().getLabels().containsKey("backupId")) {
                    String backupId = item.getMetadata().getLabels().get("backupId");
                    backupRecord.setBackupId(backupId);
                    backupRecord.setTaskName(getBackupName(clusterId, backupId).getBackupName());
                } else {
                    continue;
                }
                String backupTime = DateUtil.utc2Local(item.getMetadata().getCreationTimestamp(), DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
                backupRecord.setBackupTime(backupTime);
                backupRecord.setNamespace(item.getMetadata().getNamespace());
                backupRecord.setBackupName(item.getMetadata().getName());
                MiddlewareBackupSpec.MiddlewareBackupDestination.MiddlewareBackupParameters parameters = item.getSpec().getBackupDestination().getParameters();
                String position = item.getSpec().getBackupDestination().getDestinationType() + "(" + parameters.getUrl() + "/" + parameters.getBucket() + ")";
                backupRecord.setPosition(position);
                if (!ObjectUtils.isEmpty(backupStatus)) {
                    backupRecord.setPhrase(backupStatus.getPhase());
                    if ("Failed".equals(backupStatus.getPhase())) {
                        backupRecord.setReason(backupStatus.getReason());
                    }
                } else {
                    backupRecord.setPhrase("Unknown");
                }
                backupRecord.setSourceType(item.getMetadata().getLabels().get("type"));
                backupRecord.setAddressName(item.getMetadata().getLabels().get("addressId"));
                backupRecord.setSourceName(item.getSpec().getName());
                backupRecord.setBackupMode("single");
                backupRecord.setOwner(item.getMetadata().getLabels().get(OWNER));
                recordList.add(backupRecord);
            }
        }
        if (StringUtils.isNotBlank(keyword)) {
            return recordList.stream()
                .filter(
                    record -> StringUtils.isNotEmpty(record.getTaskName()) && record.getTaskName().contains(keyword))
                .collect(Collectors.toList());
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
                Map<String, String> labels = item.getMetadata().getLabels();
                String backupTime = DateUtil.utc2Local(item.getMetadata().getCreationTimestamp(), DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
                backupRecord.setNamespace(item.getMetadata().getNamespace());
                backupRecord.setBackupTime(backupTime);
                backupRecord.setBackupName(labels.containsKey(OWNER) ? labels.get(OWNER) : item.getMetadata().getName());
                backupRecord.setBackupFileName(item.getMetadata().getName());
                MiddlewareBackupSpec.MiddlewareBackupDestination.MiddlewareBackupParameters parameters =  item.getSpec().getBackupDestination().getParameters();
                String position = item.getSpec().getBackupDestination().getDestinationType() + "(" + parameters.getUrl() + "/" + parameters.getBucket() + ")";
                backupRecord.setPosition(position);
                if (!ObjectUtils.isEmpty(backupStatus)) {
                    if ("Failed".equals(backupStatus.getPhase())) {
                        backupRecord.setReason(backupStatus.getReason());
                    }
                    backupRecord.setUsage(calUsage(backupStatus));
                    backupRecord.setPhrase(backupStatus.getPhase());
                }
                backupRecord.setSourceName(item.getSpec().getName());
                backupRecord.setSourceType(item.getMetadata().getLabels().get("type"));
                String backupId = item.getMetadata().getLabels().get("backupId");
                // 通过schedule获取backupId
                if(StringUtils.isEmpty(backupId) && labels.containsKey(OWNER)){
                    try {
                        MiddlewareBackupScheduleCR middlewareBackupScheduleCr = backupScheduleCRDService.get(clusterId, namespace, labels.get(OWNER));
                        backupId = middlewareBackupScheduleCr.getMetadata().getLabels().get("backupId");
                    } catch (Exception e){
                        log.error("查询定时文件资源失败", e);
                        return;
                    }
                }
                backupRecord.setBackupId(backupId);
                backupRecord.setTaskName(getBackupName(clusterId, backupId).getBackupName());
                backupRecord.setAddressName(item.getMetadata().getLabels().get("addressId"));
                recordList.add(backupRecord);
            });
        }
        // 根据时间降序
        recordList.sort(
                (o1, o2) -> o1.getBackupTime() == null ? -1 : o2.getBackupTime() == null ? -1 : o2.getBackupTime().compareTo(o1.getBackupTime()));
        //添加备份记录名称
        recordList.stream().collect(Collectors.groupingBy(MiddlewareBackupRecord::getTaskName)).forEach((k, v) -> {
            for (int i = 0; i < v.size(); i++) {
                v.get(i).setRecordName(v.get(i).getTaskName() + "-" + "记录" + (i + 1));
            }
        });
        if (StringUtils.isNotBlank(keyword)) {
            return recordList.stream().filter(record -> record.getTaskName().contains(keyword)).collect(Collectors.toList());
        }
        return recordList;
    }

    @Override
    public void createBackup(MiddlewareBackupDTO backupDTO) {
        middlewareCRService.getCRAndCheckRunning(convertBackupToMiddleware(backupDTO));
        checkBackupJobName(backupDTO);
        if (MiddlewareTypeEnum.MYSQL.getType().equals(backupDTO.getType())) {
            mysqlAdapterService.createBackup(backupDTO);
        } else {
            convertMiddlewareBackup(backupDTO);
            if (StringUtils.isBlank(backupDTO.getCron())) {
                createNormalBackup(backupDTO);
                createBackupName(backupDTO.getClusterId(), backupDTO.getTaskName(), backupDTO.getLabels().get("backupId"), "normal");
            } else {
                createBackupSchedule(backupDTO);
                createBackupName(backupDTO.getClusterId(), backupDTO.getTaskName(), backupDTO.getLabels().get("backupId"), "schedule");
            }
        }
    }

    @Override
    public void updateBackupSchedule(MiddlewareBackupDTO backupDTO) {
        if (MiddlewareTypeEnum.MYSQL.getType().equals(backupDTO.getType())) {
            mysqlAdapterService.updateBackupSchedule(backupDTO);
        } else {
            MiddlewareBackupScheduleCR middlewareBackupScheduleCR = backupScheduleCRDService.get(backupDTO.getClusterId(),
                    backupDTO.getNamespace(), backupDTO.getBackupScheduleName());
            try {
                MiddlewareBackupScheduleSpec spec = middlewareBackupScheduleCR.getSpec();
                spec.getSchedule().setCron(CronUtils.parseUtcCron(backupDTO.getCron()));
                if (backupDTO.getRetentionTime() != null && StringUtils.isNotEmpty(backupDTO.getDateUnit())){
                    spec.getSchedule().setRetentionTime(calRetentionTime(backupDTO));
                    middlewareBackupScheduleCR.getMetadata().getLabels().put("unit", backupDTO.getDateUnit());
                }
                backupScheduleCRDService.update(backupDTO.getClusterId(), middlewareBackupScheduleCR);
            } catch (IOException e) {
                log.error("中间件{}备份设置更新失败", backupDTO.getMiddlewareName());
            }
        }
    }

    @Override
    public void deleteRecord(String clusterId, String namespace, String middlewareName, String type, String backupName, String backupFileName, String addressName) {
        try {
            if (MiddlewareTypeEnum.MYSQL.getType().equals(type)) {
                mysqlAdapterService.deleteRecord(clusterId, namespace, middlewareName, type, backupName, backupFileName, addressName);
            } else {
                backupCRDService.delete(clusterId, namespace, backupName);
            }
        } catch (IOException e) {
            log.error("删除备份记录失败");
        }
    }

    /**
     * 创建通用备份(定时/周期)
     * @param backupDTO
     * @return
     */
    @Override
    public void createBackupSchedule(MiddlewareBackupDTO backupDTO) {
        Minio minio = mysqlAdapterService.getMinio(backupDTO.getAddressName());
        MiddlewareBackupScheduleCR crd = new MiddlewareBackupScheduleCR();
        ObjectMeta meta = getMiddlewareBackupMeta(backupDTO.getNamespace(), backupDTO.getMiddlewareName(),
                backupDTO.getLabels(), backupDTO.getPods());
        crd.setMetadata(meta);

        // 将minio账号密码转换为base64
        String base64AccessKeyId = new String(Base64.getDecoder().decode(minio.getAccessKeyId()));
        String base64SecretAccessKey = new String(Base64.getDecoder().decode(minio.getSecretAccessKey()));
        MiddlewareBackupScheduleSpec.MiddlewareBackupScheduleDestination destination =
                new MiddlewareBackupScheduleSpec.MiddlewareBackupScheduleDestination();
        destination.setDestinationType("minio").setParameters(
                new MiddlewareBackupScheduleSpec.MiddlewareBackupScheduleDestination.MiddlewareBackupParameters(
                        minio.getBucketName(), minio.getEndpoint(), backupDTO.getType(), base64AccessKeyId, base64SecretAccessKey,
                        "MTIzNDU2Cg=="));
        List<Map<String, List<String>>> customBackups = new ArrayList<>();
        if (!backupDTO.getType().equals(MiddlewareTypeEnum.POSTGRESQL.getType())){
            Map<String, List<String>> map = new HashMap<>();
            List<String> args = new ArrayList();
            args.add("--backupSize=10");
            map.put("args", args);
            customBackups.add(map);
        }
        MiddlewareBackupScheduleSpec spec = new MiddlewareBackupScheduleSpec(destination, customBackups, backupDTO.getMiddlewareName(),
                backupDTO.getCrdType(), "off", CronUtils.parseUtcCron(backupDTO.getCron()), backupDTO.getLimitRecord(), calRetentionTime(backupDTO));
        crd.setSpec(spec);
        try {
            backupScheduleCRDService.create(backupDTO.getClusterId(), crd);
        } catch (IOException e) {
            log.error("备份创建失败", e);
        }
    }

    /**
     * 创建通用备份
     * @param backupDTO
     */
    @Override
    public void createNormalBackup(MiddlewareBackupDTO backupDTO) {
        MiddlewareBackupCR middlewareBackupCR = new MiddlewareBackupCR();
        ObjectMeta meta = getMiddlewareBackupMeta(backupDTO.getNamespace(), backupDTO.getMiddlewareName(),
                backupDTO.getLabels(), backupDTO.getPods());
        middlewareBackupCR.setMetadata(meta);
        Minio minio = mysqlAdapterService.getMinio(backupDTO.getAddressName());
        // 将minio账号密码转换为base64
        String base64AccessKeyId = new String(Base64.getDecoder().decode(minio.getAccessKeyId()));
        String base64SecretAccessKey = new String(Base64.getDecoder().decode(minio.getSecretAccessKey()));
        MiddlewareBackupSpec.MiddlewareBackupDestination destination = new MiddlewareBackupSpec.MiddlewareBackupDestination();
        destination.setDestinationType("minio").setParameters(new MiddlewareBackupSpec.MiddlewareBackupDestination.MiddlewareBackupParameters(minio.getBucketName(),
                minio.getEndpoint(), backupDTO.getType(), base64AccessKeyId, base64SecretAccessKey, "MTIzNDU2Cg=="));

        List<Map<String, Object>> customBackups = new ArrayList<>();
        if (!backupDTO.getType().equals(MiddlewareTypeEnum.POSTGRESQL.getType())){
            Map<String, Object> map = new HashMap<>();
            List<String> args = new ArrayList();
            args.add("--backupSize=10");
            map.put("args", args);
            customBackups.add(map);
        }
        MiddlewareBackupSpec spec = new MiddlewareBackupSpec(destination, backupDTO.getMiddlewareName(), backupDTO.getCrdType(), customBackups);
        middlewareBackupCR.setSpec(spec);
        try {
            backupCRDService.create(backupDTO.getClusterId(), middlewareBackupCR);
        } catch (IOException e) {
            log.error("立即备份失败", e);
        }
    }

    private Integer calRetentionTime(MiddlewareBackupDTO backupDTO) {
        if (StringUtils.isEmpty(backupDTO.getDateUnit())) {
            return backupDTO.getRetentionTime();
        }
        int retentionTime = 0;
        switch (backupDTO.getDateUnit()) {
            case "year":
                retentionTime = multi(backupDTO.getRetentionTime(), 365);
                break;
            case "month":
                retentionTime = multi(backupDTO.getRetentionTime(), 30);
                break;
            case "week":
                retentionTime = multi(backupDTO.getRetentionTime(), 7);
                break;
            case "day":
                retentionTime = backupDTO.getRetentionTime();
                break;
        }
        return retentionTime;
    }

    public static int multi(int a,int b){
        int i=0;
        int res=0;
        while(b!=0){//乘数为0则结束
            //处理乘数当前位
            if((b&1)==1){
                res+=(a<<i);
                b=b>>1;
                ++i;//i记录当前位是第几位
            }else{
                b=b>>1;
                ++i;
            }
        }
        return res;
    }

    private String calUsage(MiddlewareBackupStatus backupStatus) {
        int size = 0;
        String unit = "";
        if (!ObjectUtils.isEmpty(backupStatus)) {
            if ("Success".equals(backupStatus.getPhase())) {
                try {
                    for (int i = 0; i < backupStatus.getBackupResults().size(); i++) {
                        if (backupStatus.getBackupResults().get(i).containsKey("snapshotResults")) {
                            List results = objToList(backupStatus.getBackupResults().get(i).get("snapshotResults"));
                            for (int j = 0; j < results.size(); j++) {
                                String[] str = calUsage(String.valueOf(objectToMap(results.get(j)).get("size")));
                                size = size + Integer.valueOf(str[0]);
                                unit = str[1];
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append(size).append(".").append(unit);
        return buffer.toString();
    }

    private String[] calUsage(String size) {
        String[] be = size.split(".");
        String[] af = be[1].split(String.valueOf(be[1].charAt(1)));
        String[] str = {be[0], af[1]};
        return str;
    }

    private void checkBackupJobName(MiddlewareBackupDTO backupDTO) {
        List<MiddlewareBackupRecord> records = backupTaskList(backupDTO.getClusterId(), backupDTO.getNamespace(), null, null, null);
        records.forEach(record -> {
            if (backupDTO.getTaskName().equals(record.getTaskName())) {
                throw new BusinessException(ErrorMessage.BACKUP_JOB_NAME_ALREADY_EXISTS);
            }
        });
    }

    /**
     * 获取中间件备份Meta
     *
     * @param middlewareName 服务名称
     * @param namespace          命名空间
     * @return
     */
    public ObjectMeta getMiddlewareBackupMeta(String namespace, String middlewareName, Map<String, String> labels, List<String> pods) {
        ObjectMeta metaData = new ObjectMeta();
        metaData.setNamespace(namespace);
        metaData.setName(middlewareName + "-" + UUIDUtils.get8UUID());
        metaData.setLabels(getMiddlewareBackupLabels(middlewareName, labels, pods));
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
        Map<String, String> backupLabel = new HashMap<>();
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
            if (MiddlewareTypeEnum.MYSQL.getType().equals(type)) {
                mysqlAdapterService.createRestore(clusterId, namespace, middlewareName, type, backupName, backupFileName, pods, addressName);
            }
            else {
                MiddlewareCR cr = middlewareCRService.getCR(clusterId, namespace, type, middlewareName);
                createMiddlewareRestore(clusterId, namespace, type, middlewareName, backupName, cr.getStatus(), pods, addressName);
            }
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
        if (MiddlewareTypeEnum.MYSQL.getType().equals(type)) {
            return mysqlAdapterService.listBackupSchedule(clusterId, namespace, type, middlewareName, keyword);
        }
        if (StringUtils.isEmpty(type)) {
            recordList = mysqlAdapterService.listBackupSchedule(clusterId, namespace, type, middlewareName, keyword);
        }
        MiddlewareBackupScheduleList scheduleList = backupScheduleCRDService.list(clusterId, namespace);
        if (scheduleList != null && !CollectionUtils.isEmpty(scheduleList.getItems())) {
            for (MiddlewareBackupScheduleCR schedule : scheduleList.getItems()) {
                MiddlewareBackupScheduleStatus backupStatus = schedule.getStatus();
                MiddlewareBackupRecord backupRecord = new MiddlewareBackupRecord();
                String backupTime = DateUtil.utc2Local(schedule.getMetadata().getCreationTimestamp(), DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
                backupRecord.setBackupTime(backupTime);
                backupRecord.setNamespace(schedule.getMetadata().getNamespace());
                backupRecord.setBackupName(schedule.getMetadata().getName());
                MiddlewareBackupScheduleSpec spec = schedule.getSpec();
                MiddlewareBackupScheduleSpec.MiddlewareBackupScheduleDestination.MiddlewareBackupParameters parameters =  spec.getBackupDestination().getParameters();
                String position = spec.getBackupDestination().getDestinationType() + "(" + parameters.getUrl() + "/" + parameters.getBucket() + ")";
                backupRecord.setPosition(position);
                if (!ObjectUtils.isEmpty(backupStatus)) {
                    backupRecord.setPhrase(backupStatus.getPhase());
                } else {
                    backupRecord.setPhrase("Unknown");
                }
                backupRecord.setSourceName(schedule.getSpec().getName());
                backupRecord.setSourceType(schedule.getMetadata().getLabels().get("type"));
                backupRecord.setDateUnit(schedule.getMetadata().getLabels().get("unit"));
                String backupId = schedule.getMetadata().getLabels().get("backupId");
                backupRecord.setBackupId(backupId);
                backupRecord.setTaskName(getBackupName(clusterId, backupId).getBackupName());
                backupRecord.setAddressName(schedule.getMetadata().getLabels().get("addressId"));
                backupRecord.setCron(CronUtils.parseLocalCron(schedule.getSpec().getSchedule().getCron()));
                if (!ObjectUtils.isEmpty(schedule.getSpec().getSchedule().getRetentionTime())) {
                    backupRecord.setBackupMode("period");
                    Integer day = schedule.getSpec().getSchedule().getRetentionTime();
                    switch (backupRecord.getDateUnit()){
                        case "year":
                            backupRecord.setRetentionTime(day / 365);
                            break;
                        case "month":
                            backupRecord.setRetentionTime(day / 30);
                            break;
                        case "week":
                            backupRecord.setRetentionTime(day / 7);
                            break;
                        default:
                            backupRecord.setRetentionTime(day);
                            break;
                    }
                } else {
                    backupRecord.setBackupMode("single");
                }
                recordList.add(backupRecord);
            }
        }
        if (StringUtils.isNotBlank(keyword)) {
            return recordList.stream().filter(record -> record.getTaskName().contains(keyword)).collect(Collectors.toList());
        }
        return recordList;
    }

    @Override
    public void deleteSchedule(String clusterId, String namespace, String type, String backupScheduleName, String addressName) {
        try {
            if (MiddlewareTypeEnum.MYSQL.getType().equals(type)) {
                mysqlAdapterService.deleteSchedule(clusterId, namespace, type, backupScheduleName, addressName);
            } else {
                backupScheduleCRDService.delete(clusterId, namespace, backupScheduleName);
            }
        } catch (IOException e) {
            log.error("备份规则删除失败；{}", backupScheduleName, e);
        }
    }

    @Override
    public boolean checkIfAlreadyBackup(String clusterId, String namespace, String type, String middlewareName) {
        if (MiddlewareTypeEnum.MYSQL.getType().equals(type)) {
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
        if (MiddlewareTypeEnum.MYSQL.getType().equals(type)) {
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
    public List<MiddlewareBackupRecord> backupTaskList(String clusterId, String namespace, String middlewareName,
        String type, String keyword) {
        List<MiddlewareBackupRecord> recordList = new ArrayList<>();
        List<MiddlewareBackupRecord> backupRecords = listRecord(clusterId, namespace, middlewareName, type, keyword);
        List<MiddlewareBackupRecord> backupSchedules =
            listBackupSchedule(clusterId, namespace, type, middlewareName, keyword);
        // 过滤backupSchedule所产生的backup
        for (MiddlewareBackupRecord schedule : backupSchedules) {
            backupRecords = backupRecords.stream()
                .filter(record -> !record.getBackupName().equals(schedule.getBackupName())
                    && (StringUtils.isEmpty(record.getOwner()) || !record.getOwner().equals(schedule.getBackupName())))
                .collect(Collectors.toList());
        }
        recordList.addAll(backupRecords);
        recordList.addAll(backupSchedules);
        if (StringUtils.isNotEmpty(middlewareName)) {
            recordList = recordList.stream().filter(record -> middlewareName.equals(record.getSourceName()))
                .collect(Collectors.toList());
        }
        recordList.forEach(record -> {
            try {
                List<MiddlewareBriefInfoDTO> middlewares = middlewareService.list(clusterId, record.getNamespace(),
                    record.getSourceType(), record.getSourceName(), null);
                if (!middlewares.isEmpty()) {
                    record.setStatus(middlewares.get(0).getServiceList().get(0).getStatus());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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
    public void deleteBackUpTask(String clusterId, String namespace, String type, String backupName, String backupId, String backupFileName, String addressName, String cron) {
        if (StringUtils.isEmpty(cron)) {
            deleteRecord(clusterId, namespace, null, type, backupName, backupFileName, addressName);
        } else {
            deleteSchedule(clusterId, namespace, type, backupName, addressName);
        }
        deleteBackupName(clusterId, backupId, null);
    }

    @Override
    public void deleteBackUpRecord(String clusterId, String namespace, String type, String backupName, String backupFileName, String addressName, String backupId) {
        deleteRecord(clusterId, namespace, null, type, backupName, backupFileName, addressName);
        deleteBackupName(clusterId, backupId, "normal");
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
    public void deleteBackupName(String clusterId, String backupId, String backupType) {
        QueryWrapper<BeanMiddlewareBackupName> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(backupType)) {
            wrapper.eq("backup_type", backupType);
        }
        wrapper.eq("cluster_id", clusterId).eq("backup_id", backupId);
        middlewareBackupNameMapper.delete(wrapper);
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
        String backupId = UUIDUtils.get16UUID();
        backupLabel.put("backupId", backupId);
        backupLabel.put("addressId", backupDTO.getAddressName());
        backupLabel.put("type", backupDTO.getType());
        backupLabel.put("unit", backupDTO.getDateUnit());
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

    public List<Object> objToList(Object obj) {
        List<Object> list = new ArrayList<Object>();
        if (obj instanceof ArrayList<?>) {
            for (Object o : (List<?>) obj) {
                list.add(o);
            }
            return list;
        }
        return null;
    }

    public static Map<String, Object> objectToMap(Object obj) throws IllegalAccessException {
        Map<String, Object> map = new HashMap<String, Object>();
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Object value = field.get(obj);
            map.put(fieldName, value);
        }
        return map;
    }
}
