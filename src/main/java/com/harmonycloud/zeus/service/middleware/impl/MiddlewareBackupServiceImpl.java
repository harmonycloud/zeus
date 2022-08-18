package com.harmonycloud.zeus.service.middleware.impl;

import static com.harmonycloud.caas.common.constants.CommonConstant.INC;
import static com.harmonycloud.caas.common.constants.CommonConstant.INCR;
import static com.harmonycloud.caas.common.constants.NameConstant.OWNER;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.MiddlewareBackupDTO;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareBackupRecord;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.annotation.MiddlewareBackup;
import com.harmonycloud.zeus.bean.BeanMiddlewareBackupName;
import com.harmonycloud.zeus.dao.BeanMiddlewareBackupNameMapper;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.service.k8s.MiddlewareBackupCRService;
import com.harmonycloud.zeus.service.k8s.MiddlewareBackupScheduleCRDService;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRService;
import com.harmonycloud.zeus.service.k8s.MiddlewareRestoreCRDService;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupService;
import com.harmonycloud.zeus.service.middleware.MiddlewareCrTypeService;
import com.harmonycloud.zeus.util.CronUtils;
import com.harmonycloud.zeus.util.DateUtil;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;

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
    private MiddlewareCrTypeService middlewareCrTypeService;
    @Autowired
    private BeanMiddlewareBackupNameMapper middlewareBackupNameMapper;

    @Override
    public List<MiddlewareBackupRecord> listBackup(String clusterId, String namespace, String middlewareName,
        String type) {
        List<MiddlewareBackupRecord> recordList = new ArrayList<>();
        // 查询通用备份列表
        List<MiddlewareBackupCR> backupRecordList = getBackupRecordList(clusterId, namespace, middlewareName, type);
        if (!CollectionUtils.isEmpty(backupRecordList)) {
            for (MiddlewareBackupCR item : backupRecordList) {
                MiddlewareBackupStatus backupStatus = item.getStatus();
                MiddlewareBackupRecord backupRecord = new MiddlewareBackupRecord();
                backupRecord.setBackupId(item.getMetadata().getLabels().get("backupId"));

                // 获取备份时间
                String backupTime = DateUtil.utc2Local(item.getMetadata().getCreationTimestamp(),
                    DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
                backupRecord.setBackupTime(backupTime);
                backupRecord.setNamespace(item.getMetadata().getNamespace());
                backupRecord.setBackupName(item.getMetadata().getName());

                // 获取备份位置
                MiddlewareBackupSpec.MiddlewareBackupDestination.MiddlewareBackupParameters parameters =
                    item.getSpec().getBackupDestination().getParameters();
                String position = item.getSpec().getBackupDestination().getDestinationType() + "(" + parameters.getUrl()
                    + "/" + parameters.getBucket() + ")";
                backupRecord.setPosition(position);

                // 获取备份状态
                if (!ObjectUtils.isEmpty(backupStatus)) {
                    backupRecord.setPhrase(backupStatus.getPhase());
                    if ("Failed".equals(backupStatus.getPhase())) {
                        backupRecord.setReason(backupStatus.getReason());
                    }
                } else {
                    backupRecord.setPhrase("Unknown");
                }
                backupRecord.setSourceType(item.getMetadata().getLabels().get("type"));
                backupRecord.setAddressId(item.getMetadata().getLabels().get("addressId"));
                backupRecord.setSourceName(item.getSpec().getName());
                backupRecord.setBackupMode("single");
                backupRecord.setOwner(item.getMetadata().getLabels().get(OWNER));
                recordList.add(backupRecord);
            }
        }
        // 查询mysql备份列表(旧)
        recordList.addAll(mysqlAdapterService.listBackup(clusterId, namespace, middlewareName, type));
        return recordList;
    }

    @Override
    public void createBackup(MiddlewareBackupDTO backupDTO) {
        middlewareCRService.getCRAndCheckRunning(convertBackupToMiddleware(backupDTO));
        // todo check name exist
        checkBackupJobName(backupDTO);
        convertMiddlewareBackup(backupDTO);
        String backupType;
        if (StringUtils.isBlank(backupDTO.getCron())) {
            createNormalBackup(backupDTO);
            backupType = "normal";
        } else {
            createBackupSchedule(backupDTO);
            backupType = "schedule";
        }
        createBackupName(backupDTO.getClusterId(), backupDTO.getTaskName(), backupDTO.getLabels().get("backupId"),
            backupType);
    }

    @Override
    public void createIncBackup(String clusterId, String namespace, String backupName, String time) {
        MiddlewareBackupScheduleCR cr = backupScheduleCRDService.get(clusterId, namespace, backupName);
        cr.getMetadata().setName(backupName + "-incr");
        cr.getSpec().setMode("inc");
        cr.getSpec().getSchedule().setCron(CronUtils.convertTimeToCron(time));
        try {
            backupScheduleCRDService.create(clusterId, cr);
        } catch (Exception e){
            log.error("集群{}分区{}创建增量备份{}失败", clusterId, namespace, backupName + "incr", e);
            throw new BusinessException(ErrorMessage.CREATE_INCREMENT_BACKUP_FAILED);
        }
    }

    @Override
    public void updateBackupSchedule(MiddlewareBackupDTO backupDTO) {
        // 是否为mysqlBackup
        if (backupDTO.getMysqlBackup()) {
            mysqlAdapterService.updateBackupSchedule(backupDTO);
        } else {
            MiddlewareBackupScheduleCR middlewareBackupScheduleCR = backupScheduleCRDService
                .get(backupDTO.getClusterId(), backupDTO.getNamespace(), backupDTO.getBackupScheduleName());
            MiddlewareBackupScheduleSpec spec = middlewareBackupScheduleCR.getSpec();
            // 更新cron表达式
            spec.getSchedule().setCron(CronUtils.parseUtcCron(backupDTO.getCron()));
            // 更新备份保留时间
            if (backupDTO.getRetentionTime() != null && StringUtils.isNotEmpty(backupDTO.getDateUnit())) {
                spec.getSchedule().setRetentionTime(calRetentionTime(backupDTO));
                middlewareBackupScheduleCR.getMetadata().getLabels().put("unit", backupDTO.getDateUnit());
            }
            try {
                backupScheduleCRDService.update(backupDTO.getClusterId(), middlewareBackupScheduleCR);
            } catch (IOException e) {
                log.error("中间件{}备份设置更新失败", backupDTO.getMiddlewareName());
                throw new BusinessException(ErrorMessage.MIDDLEWARE_BACKUP_UPDATE_FAILED);
            }
            // 增量备份更新
            if (backupDTO.getIncrement() && StringUtils.isNotEmpty(backupDTO.getTime())) {
                MiddlewareBackupScheduleCR incBackupScheduleCr = backupScheduleCRDService.get(backupDTO.getClusterId(),
                    backupDTO.getNamespace(), backupDTO.getBackupScheduleName() + "-" + INCR);
                if (incBackupScheduleCr == null) {
                    throw new BusinessException(ErrorMessage.BACKUP_FILE_NOT_EXIST);
                }
                incBackupScheduleCr.getSpec().getSchedule().setCron(CronUtils.convertTimeToCron(backupDTO.getTime()));
                try {
                    backupScheduleCRDService.update(backupDTO.getClusterId(), middlewareBackupScheduleCR);
                } catch (IOException e) {
                    log.error("中间件{}增量备份设置更新失败", backupDTO.getMiddlewareName());
                    throw new BusinessException(ErrorMessage.MIDDLEWARE_BACKUP_UPDATE_FAILED);
                }
            }
        }
    }

    @Override
    public void deleteRecord(String clusterId, String namespace, String type, String backupName) {
        try {
            backupCRDService.delete(clusterId, namespace, backupName);
        } catch (Exception e) {
            if (MiddlewareTypeEnum.MYSQL.getType().equals(type)) {
                mysqlAdapterService.deleteRecord(clusterId, namespace, type, backupName);
                log.info("mysql备份删除成功");
            } else {
                log.error("删除备份记录失败");
            }
        }
    }

    /**
     * 创建通用备份(定时/周期)
     * 
     * @param backupDTO
     * @return
     */
    @Override
    public void createBackupSchedule(MiddlewareBackupDTO backupDTO) {
        Minio minio = mysqlAdapterService.getMinio(backupDTO.getAddressId());
        MiddlewareBackupScheduleCR crd = new MiddlewareBackupScheduleCR();
        ObjectMeta meta =
            getMiddlewareBackupMeta(backupDTO.getNamespace(), backupDTO.getMiddlewareName(), backupDTO.getLabels());
        crd.setMetadata(meta);

        // 将minio账号密码转换为base64
        String base64AccessKeyId =
            Base64.getEncoder().encodeToString(minio.getAccessKeyId().getBytes(StandardCharsets.UTF_8));
        String base64SecretAccessKey =
            Base64.getEncoder().encodeToString(minio.getSecretAccessKey().getBytes(StandardCharsets.UTF_8));
        MiddlewareBackupScheduleSpec.MiddlewareBackupScheduleDestination destination =
            new MiddlewareBackupScheduleSpec.MiddlewareBackupScheduleDestination();
        destination.setDestinationType("minio").setParameters(
            new MiddlewareBackupScheduleSpec.MiddlewareBackupScheduleDestination.MiddlewareBackupParameters(
                minio.getBucketName(), minio.getEndpoint(), backupDTO.getType(), base64AccessKeyId,
                base64SecretAccessKey, "MTIzNDU2Cg=="));
        List<Map<String, List<String>>> customBackups = new ArrayList<>();
        if (!backupDTO.getType().equals(MiddlewareTypeEnum.POSTGRESQL.getType())) {
            Map<String, List<String>> map = new HashMap<>();
            List<String> args = new ArrayList();
            args.add("--backupSize=10");
            map.put("args", args);
            customBackups.add(map);
        }
        MiddlewareBackupScheduleSpec spec = new MiddlewareBackupScheduleSpec(destination, customBackups,
            backupDTO.getMiddlewareName(), backupDTO.getCrdType(), "off", CronUtils.parseUtcCron(backupDTO.getCron()),
            backupDTO.getLimitRecord(), calRetentionTime(backupDTO));
        crd.setSpec(spec);
        try {
            backupScheduleCRDService.create(backupDTO.getClusterId(), crd);
        } catch (IOException e) {
            log.error("备份创建失败", e);
        }
        // 创建增量备份
        createIncBackup(backupDTO.getClusterId(), backupDTO.getNamespace(), meta.getName(), backupDTO.getTime());
    }

    /**
     * 创建通用备份
     * 
     * @param backupDTO
     */
    @Override
    public void createNormalBackup(MiddlewareBackupDTO backupDTO) {
        MiddlewareBackupCR middlewareBackupCR = new MiddlewareBackupCR();
        ObjectMeta meta =
            getMiddlewareBackupMeta(backupDTO.getNamespace(), backupDTO.getMiddlewareName(), backupDTO.getLabels());
        middlewareBackupCR.setMetadata(meta);
        Minio minio = mysqlAdapterService.getMinio(backupDTO.getAddressId());
        // 将minio账号密码转换为base64
        String base64AccessKeyId =
            Base64.getEncoder().encodeToString(minio.getAccessKeyId().getBytes(StandardCharsets.UTF_8));
        String base64SecretAccessKey =
            Base64.getEncoder().encodeToString(minio.getSecretAccessKey().getBytes(StandardCharsets.UTF_8));
        MiddlewareBackupSpec.MiddlewareBackupDestination destination =
            new MiddlewareBackupSpec.MiddlewareBackupDestination();
        destination.setDestinationType("minio")
            .setParameters(new MiddlewareBackupSpec.MiddlewareBackupDestination.MiddlewareBackupParameters(
                minio.getBucketName(), minio.getEndpoint(), backupDTO.getType(), base64AccessKeyId,
                base64SecretAccessKey, "MTIzNDU2Cg=="));

        List<Map<String, Object>> customBackups = new ArrayList<>();
        if (!backupDTO.getType().equals(MiddlewareTypeEnum.POSTGRESQL.getType())) {
            Map<String, Object> map = new HashMap<>();
            List<String> args = new ArrayList();
            args.add("--backupSize=10");
            map.put("args", args);
            customBackups.add(map);
        }
        MiddlewareBackupSpec spec =
            new MiddlewareBackupSpec(destination, backupDTO.getMiddlewareName(), backupDTO.getCrdType(), customBackups);
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

    public static int multi(int a, int b) {
        int i = 0;
        int res = 0;
        while (b != 0) {// 乘数为0则结束
            // 处理乘数当前位
            if ((b & 1) == 1) {
                res += (a << i);
                b = b >> 1;
                ++i;// i记录当前位是第几位
            } else {
                b = b >> 1;
                ++i;
            }
        }
        return res;
    }

    private String[] calUsage(String size) {
        String[] be = size.split(".");
        String[] af = be[1].split(String.valueOf(be[1].charAt(1)));
        String[] str = {be[0], af[1]};
        return str;
    }

    private void checkBackupJobName(MiddlewareBackupDTO backupDTO) {
        List<MiddlewareBackupRecord> records =
            backupTaskList(backupDTO.getClusterId(), backupDTO.getNamespace(), null, null, null);
        records.forEach(record -> {
            if (backupDTO.getTaskName().equals(record.getTaskName())) {
                throw new BusinessException(ErrorMessage.BACKUP_JOB_NAME_ALREADY_EXISTS);
            }
        });
    }

    /**
     * 获取中间件备份Meta
     *
     * @param middlewareName
     *            服务名称
     * @param namespace
     *            命名空间
     * @return
     */
    public ObjectMeta getMiddlewareBackupMeta(String namespace, String middlewareName, Map<String, String> labels) {
        ObjectMeta metaData = new ObjectMeta();
        metaData.setNamespace(namespace);
        metaData.setName(middlewareName + "-" + UUIDUtils.get8UUID());
        metaData.setLabels(labels);
        return metaData;
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
    public void createRestore(String clusterId, String namespace, String middlewareName, String type, String backupName) {
        createMiddlewareRestore(clusterId, namespace, type, middlewareName, backupName);
    }

    @Override
    public void deleteMiddlewareBackupInfo(String clusterId, String namespace, String type, String middlewareName) {
        Map<String, String> labels = getBackupLabel(middlewareName, type);
        // 删除定时备份
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
        // 删除立即备份
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
        // 删除恢复
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
    public List<MiddlewareBackupRecord> listBackupSchedule(String clusterId, String namespace, String type,
        String middlewareName) {
        // 查询定时备份cr
        MiddlewareBackupScheduleList scheduleList = backupScheduleCRDService.list(clusterId, namespace);
        if (scheduleList == null || CollectionUtils.isEmpty(scheduleList.getItems())) {
            return new ArrayList<>();
        }

        // 封装数据
        List<MiddlewareBackupRecord> recordList = new ArrayList<>();
        Map<String, String> incBackup = new HashMap<>();
        for (MiddlewareBackupScheduleCR schedule : scheduleList.getItems()) {
            // 处理增量备份数据
            if (schedule.getSpec().getMode().equals(INC)){
                incBackup.put(schedule.getMetadata().getName(), schedule.getSpec().getSchedule().getCron());
                continue;
            }
            MiddlewareBackupScheduleStatus backupStatus = schedule.getStatus();
            MiddlewareBackupRecord backupRecord = new MiddlewareBackupRecord();
            // 获取备份时间
            String backupTime = DateUtil.utc2Local(schedule.getMetadata().getCreationTimestamp(),
                    DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
            backupRecord.setBackupTime(backupTime);
            backupRecord.setNamespace(schedule.getMetadata().getNamespace());
            backupRecord.setBackupName(schedule.getMetadata().getName());
            // 获取备份位置
            MiddlewareBackupScheduleSpec spec = schedule.getSpec();
            MiddlewareBackupScheduleSpec.MiddlewareBackupScheduleDestination.MiddlewareBackupParameters parameters =
                    spec.getBackupDestination().getParameters();
            String position = spec.getBackupDestination().getDestinationType() + "(" + parameters.getUrl() + "/"
                    + parameters.getBucket() + ")";
            backupRecord.setPosition(position);
            // 获取备份状态
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
            backupRecord.setAddressId(schedule.getMetadata().getLabels().get("addressId"));
            backupRecord.setCron(CronUtils.parseLocalCron(schedule.getSpec().getSchedule().getCron()));
            // 根据单位转换备份保留时间
            if (!ObjectUtils.isEmpty(schedule.getSpec().getSchedule().getRetentionTime())) {
                backupRecord.setBackupMode("period");
                Integer day = schedule.getSpec().getSchedule().getRetentionTime();
                switch (backupRecord.getDateUnit()) {
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
        // 设置增量备份
        recordList.forEach(record -> {
            if (incBackup.containsKey(record.getBackupName() + "-" + INCR)){
                record.setIncrement(true);
                record.setTime(CronUtils.convertTimeToCron(incBackup.get(record.getBackupName() + "-" + INCR)));
            }
        });
        // 查询遗留mysqlBackup内容
        recordList.addAll(mysqlAdapterService.listBackupSchedule(clusterId, namespace, type, middlewareName));
        return recordList;
    }

    @Override
    public void deleteSchedule(String clusterId, String namespace, String type, String backupScheduleName) {
        try {
            backupScheduleCRDService.delete(clusterId, namespace, backupScheduleName);
            // 尝试删除增量备份
            try {
                backupScheduleCRDService.delete(clusterId, namespace, backupScheduleName + "-" + INCR);
            }catch (Exception ignored){
            }
        } catch (Exception e) {
            if (MiddlewareTypeEnum.MYSQL.getType().equals(type)) {
                mysqlAdapterService.deleteSchedule(clusterId, namespace, type, backupScheduleName);
            }
            log.error("定时备份删除失败；{}", backupScheduleName, e);
        }
    }

    @Override
    public boolean checkIfAlreadyBackup(String clusterId, String namespace, String type, String middlewareName) {
        if (MiddlewareTypeEnum.MYSQL.getType().equals(type)) {
            return mysqlAdapterService.checkIfAlreadyBackup(clusterId, namespace, type, middlewareName);
        }
        MiddlewareBackupScheduleList scheduleList = backupScheduleCRDService.list(clusterId, namespace);
        if (scheduleList != null && !CollectionUtils.isEmpty(scheduleList.getItems())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean checkIfAlreadyBackup(String clusterId, String namespace, String type, String middlewareName,
        String podName) {
        if (MiddlewareTypeEnum.MYSQL.getType().equals(type)) {
            return false;
        }
        List<String> pods = new ArrayList<>();
        if (StringUtils.isNotBlank(podName)) {
            pods.add(podName);
        }
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
        // 获取立即备份任务
        List<MiddlewareBackupRecord> backupRecords = listBackup(clusterId, namespace, middlewareName, type);
        // 获取定时备份任务
        List<MiddlewareBackupRecord> backupSchedules = listBackupSchedule(clusterId, namespace, type, middlewareName);
        // 过滤backupSchedule所产生的backup
        backupRecords = backupRecords.stream().filter(backupRecord -> StringUtils.isEmpty(backupRecord.getOwner()))
            .collect(Collectors.toList());

        recordList.addAll(backupRecords);
        recordList.addAll(backupSchedules);
        // 获取任务对应的中文名称
        setTaskName(recordList, clusterId);
        // 根据关键词进行过滤
        if (StringUtils.isNotEmpty(keyword)) {
            recordList = recordList.stream().filter(record -> record.getTaskName().contains(keyword))
                .collect(Collectors.toList());
        }
        // 根据中间件名称进行过滤
        if (StringUtils.isNotEmpty(middlewareName)) {
            recordList = recordList.stream().filter(record -> middlewareName.equals(record.getSourceName()))
                .collect(Collectors.toList());
        }
        // 获取运行中备份实例状态
        List<MiddlewareCR> middlewareCrList = middlewareCRService.listCR(clusterId, null, null);
        recordList.forEach(record -> {
            try {
                if (middlewareCrList.stream()
                    .noneMatch(mw -> record.getNamespace().equals(mw.getMetadata().getNamespace())
                        && record.getSourceName().equals(mw.getSpec().getName()))) {
                    record.setStatus(null);
                } else {
                    record.setStatus("Running");
                }
            } catch (Exception e) {
                log.error("集群{} 备份记录{} 获取运行中实例状态失败", clusterId, record.getBackupName());
                e.printStackTrace();
            }
        });
        // 根据时间降序
        recordList.sort((o1, o2) -> o1.getBackupTime() == null ? -1
            : o2.getBackupTime() == null ? -1 : o2.getBackupTime().compareTo(o1.getBackupTime()));
        return recordList;
    }

    @Override
    public List<MiddlewareBackupRecord> backupRecords(String clusterId, String namespace, String backupName,
        String type) {
        List<MiddlewareBackupRecord> recordList;
        // 获取所有立即备份记录 并过滤获取由指定定时备份任务产生的备份记录
        recordList = listBackup(clusterId, namespace, null, type).stream()
            .filter(record -> StringUtils.isNotEmpty(record.getOwner()) && record.getOwner().equals(backupName))
            .collect(Collectors.toList());
        // 根据时间降序
        recordList.sort((o1, o2) -> o1.getBackupTime() == null ? -1
            : o2.getBackupTime() == null ? -1 : o2.getBackupTime().compareTo(o1.getBackupTime()));
        // 获取任务对应的中文名称
        setTaskName(recordList, clusterId);
        // 设置备份记录名称
        for (int i = 0; i < recordList.size(); i++) {
            recordList.get(i).setRecordName(recordList.get(i).getTaskName() + "-" + "记录" + (i + 1));
        }
        return recordList;
    }

    @Override
    public void deleteBackUpTask(String clusterId, String namespace, String type, String backupName, String backupId,
        Boolean schedule) {
        if (schedule) {
            deleteSchedule(clusterId, namespace, type, backupName);
        } else {
            deleteRecord(clusterId, namespace, type, backupName);
        }
        deleteBackupName(clusterId, backupId, null);
    }

    @Override
    public void deleteBackUpRecord(String clusterId, String namespace, String type, String crName, String backupId) {
        // 删除备份cr
        deleteRecord(clusterId, namespace, type, crName);
        // 删除对应数据库记录
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
    public void deleteBackupName(String clusterId, String backupId, String backupType) {
        QueryWrapper<BeanMiddlewareBackupName> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(backupType)) {
            wrapper.eq("backup_type", backupType);
        }
        wrapper.eq("cluster_id", clusterId).eq("backup_id", backupId);
        middlewareBackupNameMapper.delete(wrapper);
    }

    /**
     * 查询数据库记录的备份任务名称
     */
    public void setTaskName(List<MiddlewareBackupRecord> recordList, String clusterId) {
        // 查询任务对应的中文名称
        QueryWrapper<BeanMiddlewareBackupName> wrapper =
            new QueryWrapper<BeanMiddlewareBackupName>().eq("cluster_id", clusterId);
        List<BeanMiddlewareBackupName> beanMiddlewareBackupNameList = middlewareBackupNameMapper.selectList(wrapper);
        Map<String, String> backupNameMap = beanMiddlewareBackupNameList.stream()
            .collect(Collectors.toMap(BeanMiddlewareBackupName::getBackupId, BeanMiddlewareBackupName::getBackupName));
        // 设置备份任务名称
        recordList = recordList.stream().peek(record -> {
            if (backupNameMap.containsKey(record.getBackupId())) {
                record.setTaskName(backupNameMap.get(record.getBackupId()));
            } else {
                record.setTaskName(record.getBackupName());
            }
        }).collect(Collectors.toList());
    }

    /**
     * 创建中间件恢复
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param type 中间件类型
     * @param middlewareName 源中间件名称
     * @param backupName 备份名称
     */
    public void createMiddlewareRestore(String clusterId, String namespace, String type, String middlewareName,
        String backupName) {
        MiddlewareRestoreCR crd = new MiddlewareRestoreCR();
        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace(namespace);
        meta.setName(getRestoreName(type, middlewareName));
        // 设置对应label
        Map<String, String> backupLabel = getBackupLabel(middlewareName, type);
        Map<String, String> middlewareLabel = getBackupLabel(middlewareName, type);
        backupLabel.putAll(middlewareLabel);
        meta.setLabels(backupLabel);
        crd.setMetadata(meta);

        MiddlewareRestoreSpec spec = new MiddlewareRestoreSpec();
        List<String> args = new ArrayList<>();
        args.add("--backupResultName=" + backupName);
        args.add("--backupNamespace=" + namespace);
        List<Map<String, List<String>>> customRestores = new ArrayList<>();
        Map<String, List<String>> map = new HashMap<>();
        map.put("args", args);
        customRestores.add(map);

        spec.setName(middlewareName);
        spec.setType(middlewareCrTypeService.findByType(type));
        spec.setCustomRestores(customRestores);
        crd.setSpec(spec);
        try {
            restoreCRDService.create(clusterId, crd);
        } catch (Exception e){
            log.error("集群{} 中间件{} 克隆实例失败", clusterId, middlewareName, e);
            throw new BusinessException(ErrorMessage.BACKUP_RESTORE_FAILED);
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

        String middlewareCrdType = middlewareCrTypeService.findByType(type);
        Map<String, String> backupLabel = getBackupLabel(middlewareName, type);
        String backupId = UUIDUtils.get16UUID();
        backupLabel.put("backupId", backupId);
        backupLabel.put("addressId", backupDTO.getAddressId());
        backupLabel.put("type", backupDTO.getType());
        backupLabel.put("unit", backupDTO.getDateUnit());
        backupDTO.setLabels(backupLabel);
        backupDTO.setCrdType(middlewareCrdType);
    }

    /**
     * 查询备份记录(立即备份)
     */
    private List<MiddlewareBackupCR> getBackupRecordList(String clusterId, String namespace, String middlewareName,
        String type) {
        List<MiddlewareBackupCR> resList = new ArrayList<>();
        // 查询所有即时备份创建的备份记录
        MiddlewareBackupList backupList;
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
    private Middleware convertBackupToMiddleware(MiddlewareBackupDTO backupDTO) {
        return new Middleware().setClusterId(backupDTO.getClusterId()).setNamespace(backupDTO.getNamespace())
            .setType(backupDTO.getType()).setName(backupDTO.getMiddlewareName());
    }

}
