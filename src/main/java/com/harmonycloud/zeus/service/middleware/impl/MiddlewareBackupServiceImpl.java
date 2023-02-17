package com.harmonycloud.zeus.service.middleware.impl;

import static com.harmonycloud.caas.common.constants.BackupConstant.*;
import static com.harmonycloud.caas.common.constants.CommonConstant.INCR;
import static com.harmonycloud.caas.common.constants.NameConstant.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.constants.ActiveAreaConstant;
import com.harmonycloud.caas.common.enums.*;
import com.harmonycloud.caas.common.model.*;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.zeus.bean.*;
import com.harmonycloud.zeus.bean.user.BeanUserRole;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.middleware.*;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.service.user.ProjectService;
import com.harmonycloud.zeus.service.user.RoleAuthorityService;
import com.harmonycloud.zeus.service.user.UserRoleService;
import com.harmonycloud.zeus.util.RequestUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.annotation.MiddlewareBackup;
import com.harmonycloud.zeus.dao.BeanMiddlewareBackupNameMapper;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.util.CronUtils;

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

    @Value("${system.cron.timezone: 0}")
    private Integer timezone;

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
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private BackupPositionService backupPositionService;
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private ActiveAreaService activeAreaService;
    @Autowired
    private BackupServerService backupServerService;
    @Autowired
    private RoleAuthorityService roleAuthorityService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private MiddlewareInfoService middlewareInfoService;
    @Autowired
    private MiddlewareBackupNameService backupNameService;

    @Override
    public List<MiddlewareBackupRecord> listBackup(String clusterId, String namespace, String middlewareName,
        String type) {
        List<MiddlewareBackupRecord> recordList = new ArrayList<>();
        // 查询通用备份列表
        List<MiddlewareBackupCR> backupRecordList = getBackupRecordList(clusterId, namespace, middlewareName, type);
        if (!CollectionUtils.isEmpty(backupRecordList)) {
            for (MiddlewareBackupCR item : backupRecordList) {
                MiddlewareBackupRecord backupRecord = new MiddlewareBackupRecord();
                convertBackupToRecord(item, backupRecord);
                recordList.add(backupRecord);
            }
        }
        // 查询mysql备份列表(旧)
        recordList.addAll(mysqlAdapterService.listBackup(clusterId, namespace, middlewareName, type));
        return recordList;
    }

    @Override
    public MiddlewareBackupRecord getBackup(String clusterId, String namespace, String backupName, String backupMode) {
        MiddlewareBackupRecord record = new MiddlewareBackupRecord();
        if (BackupMode.PERIOD.getMode().equals(backupMode)) {
            MiddlewareBackupScheduleCR scheduleCR = backupScheduleCRDService.get(clusterId, namespace, backupName);
            convertBackupScheduleToRecord(scheduleCR, record);
        } else {
            MiddlewareBackupCR backupCR = backupCRDService.get(clusterId, namespace, backupName);
            convertBackupToRecord(backupCR, record);
        }
        return record;
    }

    @Override
    public void createBackup(MiddlewareBackupDTO backupDTO) {
        if (backupDTO.getIncrement() != null && backupDTO.getIncrement()) {
            checkTimeLawful(backupDTO.getCron(), backupDTO.getRetentionTime());
        }
        middlewareCRService.getCRAndCheckRunning(convertBackupToMiddleware(backupDTO));
        // check name exist
        this.checkBackupJobName(backupDTO);
        this.convertMiddlewareBackup(backupDTO);
        // 根据备份任务类型创建备份任务
        this.createBackupByTaskType(backupDTO);
        // 保存备份任务名称到数据库
        this.saveBackupName(backupDTO);
    }

    private void checkTimeLawful(String cronStr, Integer retentionTime) {
        if (cronStr == null) {
            throw new BusinessException(DictEnum.BACKUP_SCHEDULE_CRON, ErrorMessage.NOT_FOUND);
        }
        if (retentionTime == null) {
            throw new BusinessException(DictEnum.BACKUP_RETENTION_TIME, ErrorMessage.NOT_FOUND);
        }
        String[] crons = cronStr.split(" ");
        String weekStr = crons[crons.length - 1];
        String[] _weeks = weekStr.split(",");
        Integer[] week = new Integer[_weeks.length];
        for (int i = 0; i < _weeks.length; i++) {
            week[i] = Integer.valueOf(_weeks[i]);
        }
        int max = week[0] - week[week.length - 1] + 7;
        for (int i = 1; i < week.length; i++) {
            int j = week[i] - week[i - 1];
            max = Math.max(max, j);
        }
        if (max > retentionTime) {
            throw new BusinessException(ErrorMessage.MIDDLEWARE_BACKUP_CRON_ILLEGAL);
        }
    }

    @Override
    public void createIncBackup(String clusterId, String namespace, String backupName, String time) {
        // 校验备份周期和保留时间
        MiddlewareBackupScheduleCR baks = backupScheduleCRDService.get(clusterId, namespace, backupName);
        if (baks == null || baks.getSpec() == null || baks.getSpec().getSchedule() == null) {
            throw new BusinessException(ErrorMessage.FIND_BACKUP_SCHEDULE_CRON_FAILED);
        }
        String cron = baks.getSpec().getSchedule().getCron();
        Integer retentionTime = baks.getSpec().getSchedule().getRetentionTime();
        checkTimeLawful(cron, retentionTime);
        createIncBackup(clusterId, namespace, backupName, time, null);
    }

    @Override
    public void createIncBackup(String clusterId, String namespace, String backupName, String time, MiddlewareBackupScheduleCR scheduleCR) {
        MiddlewareIncBackup incBackup = new MiddlewareIncBackup();
        incBackup.setClusterId(clusterId);
        incBackup.setNamespace(namespace);
        incBackup.setBackupName(backupName);
        incBackup.setTime(time);
        ObjectMeta meta = new ObjectMeta();
        if (namespaceService.isOpenAvailableDomain(clusterId, namespace)) {
            //如果是双活分区，则从middlewarebackupschedule cr里获取选择器annotation和labels
            if (scheduleCR == null) {
                scheduleCR = backupScheduleCRDService.get(clusterId, namespace, backupName);
            }
            Map<String, String> annotations = getAreaSelectorAnnotations(scheduleCR.getMetadata().getAnnotations());
            Map<String, String> areaLabels = getAreaLabels(scheduleCR.getMetadata().getLabels());
            meta.setAnnotations(annotations);
            meta.setLabels(areaLabels);
        }
        createIncBackupSchedule(incBackup, meta);
    }

    @Override
    public void updateBackupSchedule(MiddlewareBackupDTO backupDTO) {
        // 是否为mysqlBackup
        if (backupDTO.getMysqlBackup() != null && backupDTO.getMysqlBackup()) {
            mysqlAdapterService.updateBackupSchedule(backupDTO);
        } else {
            MiddlewareBackupScheduleCR middlewareBackupScheduleCR = backupScheduleCRDService
                .get(backupDTO.getClusterId(), backupDTO.getNamespace(), backupDTO.getBackupName());
            MiddlewareBackupScheduleSpec spec = middlewareBackupScheduleCR.getSpec();
            // 更新cron表达式
            if (StringUtils.isNotEmpty(backupDTO.getCron())) {
                spec.getSchedule().setCron(CronUtils.parseCron(backupDTO.getCron(), -8 + timezone));
            }
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
            if (backupDTO.getIncrement() != null && backupDTO.getIncrement()) {
                MiddlewareBackupScheduleCR incBackupScheduleCr = backupScheduleCRDService.get(backupDTO.getClusterId(),
                    backupDTO.getNamespace(), backupDTO.getBackupName() + "-" + INCR);
                if (incBackupScheduleCr == null) {
                    throw new BusinessException(ErrorMessage.BACKUP_FILE_NOT_EXIST);
                }
                // 更新开启/关闭
                if (backupDTO.getTurnOff() != null && backupDTO.getTurnOff()) {
                    incBackupScheduleCr.getSpec().setPause("on");
                }
                // 更新时间(cron)
                if (StringUtils.isNotEmpty(backupDTO.getTime())) {
                    incBackupScheduleCr.getSpec().getSchedule()
                        .setCron(CronUtils.convertTimeToCron(backupDTO.getTime()));
                }
                // 更新备份保留时间
                if (backupDTO.getRetentionTime() != null && StringUtils.isNotEmpty(backupDTO.getDateUnit())) {
                    incBackupScheduleCr.getSpec().getSchedule().setRetentionTime(calRetentionTime(backupDTO));
                }
                try {
                    backupScheduleCRDService.update(backupDTO.getClusterId(), incBackupScheduleCr);
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
     * @param minio
     * @param objectMeta
     * @return
     */
    @Override
    public void createBackupSchedule(MiddlewareBackupDTO backupDTO, Minio minio, ObjectMeta objectMeta) {
        checkBackupScheduleExist(backupDTO);
        MiddlewareBackupScheduleCR crd = new MiddlewareBackupScheduleCR();
        ObjectMeta meta = getMiddlewareBackupMeta(backupDTO, objectMeta);
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
        // 设置备份类型(全量备份)
        List<Map<String, List<Map<String, String>>>> customBackups = new ArrayList<>();
        Map<String, List<Map<String, String>>> env = new HashMap<>();
        Map<String, String> map = new HashMap<>();
        map.put(NAME, OPERATION_TYPE);
        map.put(VALUE, BACKUP);
        List<Map<String, String>> envList = new ArrayList<>();
        envList.add(map);
        env.put(ENV, envList);
        customBackups.add(env);

        MiddlewareBackupScheduleSpec spec =
            new MiddlewareBackupScheduleSpec(destination, customBackups, backupDTO.getMiddlewareName(),
                backupDTO.getCrdType(), "off", CronUtils.parseCron(backupDTO.getCron(), -8 + timezone),
                backupDTO.getLimitRecord(), calRetentionTime(backupDTO));
        crd.setSpec(spec);
        try {
            backupScheduleCRDService.create(backupDTO.getClusterId(), crd);
        } catch (IOException e) {
            log.error("备份创建失败", e);
        }
        // 创建增量备份
        if (backupDTO.getIncrement() != null && StringUtils.isNotEmpty(backupDTO.getTime()) && backupDTO.getIncrement()) {
            createIncBackup(backupDTO.getClusterId(), backupDTO.getNamespace(), meta.getName(), backupDTO.getTime(), crd);
        }
    }

    /**
     * 创建通用备份
     *  @param backupDTO
     * @param minio
     * @param objectMeta
     */
    @Override
    public void createNormalBackup(MiddlewareBackupDTO backupDTO, Minio minio, ObjectMeta objectMeta) {
        MiddlewareBackupCR middlewareBackupCR = new MiddlewareBackupCR();
        ObjectMeta meta = getMiddlewareBackupMeta(backupDTO, objectMeta);
        middlewareBackupCR.setMetadata(meta);
        // 将minio账号密码转换为base64
        String base64AccessKeyId =
            Base64.getEncoder().encodeToString(minio.getAccessKeyId().getBytes(StandardCharsets.UTF_8));
        String base64SecretAccessKey =
            Base64.getEncoder().encodeToString(minio.getSecretAccessKey().getBytes(StandardCharsets.UTF_8));
        MiddlewareBackupSpec.MiddlewareBackupDestination destination =
            new MiddlewareBackupSpec.MiddlewareBackupDestination();
        destination.setDestinationType("minio")
            .setParameters(new MiddlewareBackupSpec.MiddlewareBackupDestination.MiddlewareBackupParameters(
                minio.getBucketName(), minio.getEndpoint(), meta.getName(), base64AccessKeyId,
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

    /**
     * 创建增量备份
     * @param middlewareIncBackup
     * @param objectMeta
     */
    @Override
    public void createIncBackupSchedule(MiddlewareIncBackup middlewareIncBackup, ObjectMeta objectMeta) {
        String clusterId = middlewareIncBackup.getClusterId();
        String namespace = middlewareIncBackup.getNamespace();
        String backupName = middlewareIncBackup.getBackupName();
        String time = middlewareIncBackup.getTime();
        Map<String, String> annotations = middlewareIncBackup.getAnnotations();
        MiddlewareBackupScheduleCR cr = backupScheduleCRDService.get(clusterId, namespace, backupName);
        objectMeta.setName(backupName + "-" + INCR);
        objectMeta.setNamespace(namespace);
        // 获取annotations
        if (objectMeta.getAnnotations() == null) {
            objectMeta.setAnnotations(new HashMap<>());
        }
        if (annotations != null) {
            objectMeta.getAnnotations().putAll(annotations);
        }
        // 获取labels
        Map<String, String> backupLabel = objectMeta.getLabels();
        if (backupLabel == null) {
            backupLabel = new HashMap<>();
            objectMeta.setLabels(backupLabel);
        }
        backupLabel.put("middleware", cr.getSpec().getType() + "-" + cr.getSpec().getName());

        cr.setMetadata(objectMeta);
        cr.setStatus(null);
        // 设置增量备份
        cr.getSpec().getCustomBackups().forEach(cus -> {
            if (cus.containsKey(ENV)){
                cus.get(ENV).forEach(env -> {
                    if (env.containsKey(VALUE) && env.get(VALUE).equals(BACKUP)){
                        env.put(VALUE, BACKUP_INC);
                    }
                });
            }
        });
        // 转换时间单位
        cr.getSpec().getSchedule().setCron(CronUtils.convertTimeToCron(time));
        try {
            backupScheduleCRDService.create(clusterId, cr);
        } catch (Exception e){
            log.error("集群{}分区{}创建增量备份{}失败", clusterId, namespace, backupName + "-incr", e);
            throw new BusinessException(ErrorMessage.CREATE_INCREMENT_BACKUP_FAILED);
        }
    }

    /**
     * 根据备份任务类型（是否双活）创建备份任务
     * @param backupDTO
     */
    private void createBackupByTaskType(MiddlewareBackupDTO backupDTO) {
        if (activeActiveBackupCheck(backupDTO)) {
            // 双活备份
            // 获取可用区annotation
            ActiveAreaAnnotationDto activeAreaAnnotation = middlewareService.getActiveAreaAnnotation(backupDTO.getClusterId(),
                    backupDTO.getNamespace(), backupDTO.getType(), backupDTO.getMiddlewareName());
            // 创建A可用区备份
            createBackupTask(backupDTO, backupPositionService.getMinio(backupDTO.getBackupPositionId(), ServerUsageEnum.zoneA.getName()),
                    getActiveAreaObjectMeta(activeAreaAnnotation, ServerUsageEnum.zoneA.getName()));
            // 创建B可用区备份
            createBackupTask(backupDTO, backupPositionService.getMinio(backupDTO.getBackupPositionId(), ServerUsageEnum.zoneB.getName()),
                    getActiveAreaObjectMeta(activeAreaAnnotation, ServerUsageEnum.zoneB.getName()));
        } else {
            // 普通备份
            createBackupTask(backupDTO, backupPositionService.getMinio(backupDTO.getBackupPositionId(), null), new ObjectMeta());
        }
    }

    /**
     * 创建备份任务
     * @param backupDTO
     * @param minio
     */
    private void createBackupTask(MiddlewareBackupDTO backupDTO, Minio minio, ObjectMeta objectMeta) {
        if (StringUtils.isEmpty(backupDTO.getCron())) {
            createNormalBackup(backupDTO, minio, objectMeta);
        } else {
            createBackupSchedule(backupDTO, minio, objectMeta);
        }
    }

    /**
     * 返回双活ObjectMeta
     * @param activeAreaAnnotationDto
     * @param serverUsage A：可用区A，B：可用区B
     */
    private ObjectMeta getActiveAreaObjectMeta(ActiveAreaAnnotationDto activeAreaAnnotationDto, String serverUsage) {
        ObjectMeta objectMeta = new ObjectMeta();
        Map<String, String> annotations = new HashMap<>();
        objectMeta.setAnnotations(annotations);
        Map<String, String> labels = new HashMap<>();
        objectMeta.setLabels(labels);

        if (serverUsage.equals(ServerUsageEnum.zoneA.getName())) {
            annotations.putAll(activeAreaAnnotationDto.getZoneAAnnotation());
            labels.put("activeArea", ActiveAreaEnum.zoneA.getName());
        } else {
            annotations.putAll(activeAreaAnnotationDto.getZoneBAnnotation());
            labels.put("activeArea", ActiveAreaEnum.zoneB.getName());
        }
        return objectMeta;
    }

    /**
     * 检查是否是双活备份任务,当且仅当分区为双活分区，并且是pg、redis、mysql，并且是双活备份位置的时候才是双活备份任务
     * @param backupDTO
     * @return
     */
    private boolean activeActiveBackupCheck(MiddlewareBackupDTO backupDTO) {
        boolean activeActiveNamespace = namespaceService.isOpenAvailableDomain(backupDTO.getClusterId(), backupDTO.getNamespace());
        BeanBackupServer backupServer = backupPositionService.getBackupServer(backupDTO.getBackupPositionId());
        if (activeActiveNamespace && backupServer.getType() == 2) {
            String type = backupDTO.getType();
            return type.equals(MiddlewareTypeEnum.MYSQL.getType()) || type.equals(MiddlewareTypeEnum.POSTGRESQL.getType()) || type.equals(MiddlewareTypeEnum.REDIS.getType());
        }
        return false;
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
            if (StringUtils.isNotEmpty(backupDTO.getTaskName()) && backupDTO.getTaskName().equals(record.getTaskName())) {
                throw new BusinessException(ErrorMessage.BACKUP_JOB_NAME_ALREADY_EXISTS);
            }
        });
    }

    /**
     * 设置中间件备份meta
     * @param backupDTO
     * @return
     */
    public ObjectMeta getMiddlewareBackupMeta(MiddlewareBackupDTO backupDTO, ObjectMeta metaData) {
        if (metaData == null) {
            metaData = new ObjectMeta();
        }
        if (metaData.getLabels() == null) {
            metaData.setLabels(new HashMap<>());
        }
        if (metaData.getAnnotations() == null) {
            metaData.setAnnotations(new HashMap<>());
        }
        metaData.setNamespace(backupDTO.getNamespace());
        metaData.setName(backupDTO.getMiddlewareName() + "-" + UUIDUtils.get8UUID());
        if (backupDTO.getLabels() != null) {
            metaData.getLabels().putAll(backupDTO.getLabels());
        }
        if (backupDTO.getAnnotations() != null) {
            metaData.getAnnotations().putAll(backupDTO.getAnnotations());
        }
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

    @Override
    public void createRestore(String clusterId, String namespace, String middlewareName, String type, String backupName, String restoreTime) {
        // 等待中间件状态正常
        if (!waitingMiddleware(clusterId, namespace, middlewareName, type)){
            return;
        }
        MiddlewareRestoreCR crd = new MiddlewareRestoreCR();
        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace(namespace);
        meta.setName(middlewareName + "-restore");
        // 设置对应label
        Map<String, String> backupLabel = getBackupLabel(middlewareName, type);
        meta.setLabels(backupLabel);
        crd.setMetadata(meta);

        MiddlewareRestoreSpec spec = new MiddlewareRestoreSpec();
        List<String> args = new ArrayList<>();
        List<Map<String, String>> envList = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        args.add("--backupNamespace=" + namespace);
        if (StringUtils.isEmpty(restoreTime)){
            if (MiddlewareTypeEnum.POSTGRESQL.getType().equals(type) || MiddlewareTypeEnum.MYSQL.getType().equals(type)){
                args.add("--mode=full");
            }
            args.add("--backupResultName=" + backupName);
        }else {
            args.add("--mode=inc");
            args.add("--backupResultName=" + backupName + "-incr");
            Map<String, String> envMap = new HashMap<>();
            envMap.put(NAME, RESTORE_TIME);
            // 时间格式转换
            Date date = DateUtils.parseDate(restoreTime, DateType.YYYY_MM_DD_HH_MM_SS.getValue());
            envMap.put(VALUE, DateUtils.dateToString(DateUtils.addInteger(date, Calendar.HOUR_OF_DAY, -8),
                DateUtils.YYYY_MM_DD_T_HH_MM_SS_Z));
            envList.add(envMap);
            map.put(ENV, envList);
        }
        List<Map<String, Object>> customRestores = new ArrayList<>();
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

    @Override
    public void deleteMiddlewareBackupInfo(String clusterId, String namespace, String type, String middlewareName) {
        Map<String, String> labels = getBackupLabel(middlewareName, type);
        // 删除定时备份
        List<MiddlewareBackupScheduleCR> middlewareBackupScheduleCRList =
            backupScheduleCRDService.listByLabels(clusterId, namespace, labels);
        middlewareBackupScheduleCRList.forEach(item -> {
            try {
                backupScheduleCRDService.delete(clusterId, namespace, item.getMetadata().getName());
            } catch (IOException e) {
                log.error("删除定时备份失败");
            }
        });
        // 删除立即备份
        List<MiddlewareBackupCR> backupCRList = backupCRDService.list(clusterId, namespace, labels);
        if (!CollectionUtils.isEmpty(backupCRList)) {
            backupCRList.forEach(item -> {
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
        Map<String, MiddlewareBackupScheduleCR> incBackup = new HashMap<>();
        for (MiddlewareBackupScheduleCR schedule : scheduleList.getItems()) {
            // 处理增量备份数据
            if (isIncBackup(schedule)){
                incBackup.put(schedule.getMetadata().getName(), schedule);
                continue;
            }

            MiddlewareBackupRecord backupRecord = new MiddlewareBackupRecord();
            convertBackupScheduleToRecord(schedule, backupRecord);
            recordList.add(backupRecord);
        }
        // 设置增量备份
        recordList.forEach(record -> {
            if (incBackup.containsKey(record.getBackupName() + "-" + INCR)) {
                record.setIncrement(true);
                MiddlewareBackupScheduleCR incSchedule = incBackup.get(record.getBackupName() + "-" + INCR);
                record.setTime(CronUtils.convertCronToTime(incSchedule.getSpec().getSchedule().getCron()));
                Date backupTime = checkTimeExist(incSchedule);
                if (backupTime != null) {
                    record.setBackupTime(backupTime);
                }
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
     * @return
     */
    @Override
    public List<MiddlewareBackupRecord> backupTaskList(String clusterId, String namespace, String middlewareName,
                                                       String type, String keyword) {
        List<MiddlewareBackupRecord> recordList = new ArrayList<>();
        // 获取立即备份任务
        List<MiddlewareBackupRecord> backupRecords = listBackup(clusterId, namespace, middlewareName, type);
        // 获取定时备份任务
        List<MiddlewareBackupRecord> backupSchedules = listBackupSchedule(clusterId, namespace, type, middlewareName);
        // 获取备份任务的最近备份时间
        setBackupScheduleBackupTime(backupSchedules, backupRecords);
        // 过滤backupSchedule所产生的backup
        backupRecords = backupRecords.stream().filter(backupRecord -> StringUtils.isEmpty(backupRecord.getOwner()))
            .filter(backupRecord -> StringUtils.isEmpty(backupRecord.getPhrase())
                || !"Deleting".equals(backupRecord.getPhrase()))
            .collect(Collectors.toList());

        recordList.addAll(backupRecords);
        recordList.addAll(backupSchedules);
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

        return recordList;
    }

    @Override
    public List<MiddlewareBackupRecordGroup> backupTaskGroupList(String clusterId, String namespace, String middlewareName, String type, String keyword) {
        List<MiddlewareBackupRecord> records = backupTaskList(clusterId, namespace, middlewareName, type, keyword);
        // 根据项目过滤中间件
        records = filterByProject(records);
        // 设置备份地址
        setBackupPosition(records);
        List<MiddlewareBackupRecordGroup> recordGroups = groupByBackupId(clusterId,records);
        setMiddlewareStatus(clusterId,recordGroups);
        return recordGroups;
    }

    @Override
    public MiddlewareIncBackupDto getIncBackupInfo(String clusterId, String namespace, String backupName) {
        MiddlewareBackupScheduleCR cr = backupScheduleCRDService.get(clusterId, namespace, backupName + "-incr");
        MiddlewareIncBackupDto middlewareIncBackupDto = new MiddlewareIncBackupDto();
        if (cr == null) {
            middlewareIncBackupDto.setBackupName(backupName + "-incr");
            return middlewareIncBackupDto;
        }
        // 获取时间
        if (cr.getStatus() != null && cr.getStatus().getStorageProvider() != null) {
            JSONObject storageProvider = cr.getStatus().getStorageProvider();
            String type = middlewareCrTypeService.findTypeByCrType(cr.getSpec().getType());
            JSONObject time = storageProvider.getJSONObject(type);

            if (time != null && time.containsKey("startTime") && time.containsKey("endTime")) {
                Date startTime = DateUtils.parseUTCDate(time.getString("startTime"));
                Date endTime = DateUtils.parseUTCDate(time.getString("endTime"));
                middlewareIncBackupDto.setStartTime(startTime).setEndTime(endTime);
            }
        }

        // 封装数据
        middlewareIncBackupDto.setPause(cr.getSpec().getPause())
            .setTime(CronUtils.convertCronToTime(cr.getSpec().getSchedule().getCron()));
        return middlewareIncBackupDto;
    }

    @Override
    public List<MiddlewareBackupRecord> backupRecords(String clusterId, String namespace, String backupName,
        String type) {
        List<MiddlewareBackupRecord> recordList;
        // 获取所有立即备份记录 并过滤获取由指定定时备份任务产生的备份记录
        recordList = listBackup(clusterId, namespace, null, null).stream()
            .filter(record -> (StringUtils.isNotEmpty(record.getOwner()) && record.getOwner().equals(backupName))
                || (StringUtils.isNotEmpty(record.getBackupName()) && record.getBackupName().equals(backupName)))
            .collect(Collectors.toList());
        // 获取backupId
        String backupId = null;
        MiddlewareBackupScheduleCR cr = backupScheduleCRDService.get(clusterId, namespace, backupName);
        if (cr != null && !CollectionUtils.isEmpty(cr.getMetadata().getLabels()) && cr.getMetadata().getLabels().containsKey(BACKUP_ID)){
            backupId = cr.getMetadata().getLabels().get(BACKUP_ID);
        }
        // 根据时间降序
        recordList.sort((o1, o2) -> o1.getBackupTime() == null ? -1
            : o2.getBackupTime() == null ? -1 : o2.getBackupTime().compareTo(o1.getBackupTime()));
        // 获取任务对应的中文名称
        setTaskName(recordList, clusterId, backupId);
        // 设置备份记录名称
        for (int i = 0; i < recordList.size(); i++) {
            recordList.get(i).setRecordName(recordList.get(i).getTaskName() + "-" + "记录" + (i + 1));
        }
        return recordList;
    }

    @Override
    public void deleteBackUpTask(MiddlewareTaskDTO taskDTO) {
        String clusterId = taskDTO.getClusterId();
        String namespace = taskDTO.getNamespace();
        String type = taskDTO.getType();
        String backupId = taskDTO.getBackupId();
        List<String> backupNameList = taskDTO.getBackupNameList();
        backupNameList.forEach(backupName ->{
            if ("period".equals(taskDTO.getBackupMode())) {
                deleteSchedule(clusterId, namespace, type, backupName);
            } else {
                deleteRecord(clusterId, namespace, type, backupName);
            }
        });
        if (StringUtils.isNotEmpty(backupId)){
            deleteBackupName(clusterId, backupId);
        }
    }

    @Override
    public void deleteBackUpRecord(String clusterId, String namespace, String type, String backupName, String backupId) {
        // 删除备份cr
        deleteRecord(clusterId, namespace, type, backupName);
        // 删除对应数据库记录
        if (StringUtils.isNotEmpty(backupId)){
            deleteBackupName(clusterId, backupId);
        }
    }

    @Override
    public void saveBackupName(String clusterId, String taskName, String backupId, String backupType, Integer positionId) {
        BeanMiddlewareBackupName backupName = new BeanMiddlewareBackupName();
        backupName.setBackupName(taskName);
        backupName.setBackupId(backupId);
        backupName.setClusterId(clusterId);
        backupName.setBackupType(backupType);
        backupName.setPositionId(positionId);
        middlewareBackupNameMapper.insert(backupName);
    }

    public void deleteBackupName(String clusterId, String backupId) {
        QueryWrapper<BeanMiddlewareBackupName> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id", clusterId).eq("backup_id", backupId);
        middlewareBackupNameMapper.delete(wrapper);
    }

    @Override
    public List<MiddlewareBackupRecord> listBackupTask(String clusterId, String namespace, Map<String, String> labels) {
        List<MiddlewareBackupRecord> records = new ArrayList<>();
        List<MiddlewareBackupScheduleCR> scheduleCRS = backupScheduleCRDService.listByLabels(clusterId, namespace, labels);
        if (!CollectionUtils.isEmpty(scheduleCRS)) {
            for (MiddlewareBackupScheduleCR scheduleCR : scheduleCRS) {
                MiddlewareBackupRecord record = new MiddlewareBackupRecord();
                convertBackupScheduleToRecord(scheduleCR, record);
                records.add(record);
            }
        }
        List<MiddlewareBackupCR> backupCRList = backupCRDService.list(clusterId, namespace, labels);
        if (!CollectionUtils.isEmpty(backupCRList)) {
            for (MiddlewareBackupCR backupCR : backupCRList) {
                MiddlewareBackupRecord record = new MiddlewareBackupRecord();
                convertBackupToRecord(backupCR, record);
                records.add(record);
            }
        }
        return records;
    }

    @Override
    public boolean checkSchedule(String clusterId, String namespace, String type, String middlewareName) {
        return checkBackupScheduleExist(clusterId, namespace, middlewareName, null);
    }

    /**
     * 查询数据库记录的备份任务名称
     */
    public void setTaskName(List<MiddlewareBackupRecord> recordList, String clusterId, String backupId) {
        // 查询任务对应的中文名称
        QueryWrapper<BeanMiddlewareBackupName> wrapper =
            new QueryWrapper<BeanMiddlewareBackupName>().eq("cluster_id", clusterId);
        List<BeanMiddlewareBackupName> beanMiddlewareBackupNameList = middlewareBackupNameMapper.selectList(wrapper);
        Map<String, String> backupNameMap = beanMiddlewareBackupNameList.stream()
            .collect(Collectors.toMap(BeanMiddlewareBackupName::getBackupId, BeanMiddlewareBackupName::getBackupName));
        // 设置备份任务名称
        for (MiddlewareBackupRecord record : recordList) {
            if (StringUtils.isNotEmpty(record.getTaskName())) {
                continue;
            }
            if (StringUtils.isNotEmpty(record.getBackupId()) && backupNameMap.containsKey(record.getBackupId())) {
                record.setTaskName(backupNameMap.get(record.getBackupId()));
            } else if (StringUtils.isNotEmpty(backupId) && backupNameMap.containsKey(backupId)) {
                record.setTaskName(backupNameMap.get(backupId));
            } else {
                record.setTaskName(record.getBackupName());
            }
        }
    }


    /**
     * 设置备份任务所属可用区别名
     */
    private void setAreaAliasName(String clusterId, List<MiddlewareBackupRecord> recordList) {
        recordList.forEach(record -> {
            if (StringUtils.isNotEmpty(record.getActiveArea())) {
                BeanActiveArea activeArea = activeAreaService.get(clusterId, record.getActiveArea());
                if (activeArea != null) {
                    record.setAreaAliasName(activeArea.getAliasName());
                }
            }
        });
    }

    /**
     * 设置中间件状态
     * @param clusterId
     * @param recordGroups
     */
    private void setMiddlewareStatus(String clusterId, List<MiddlewareBackupRecordGroup> recordGroups) {
        MiddlewareClusterDTO clusterDTO = clusterService.findById(clusterId);
        recordGroups.forEach(recordGroup -> {
            String sourceName = recordGroup.getSourceName();
            String namespace = recordGroup.getNamespace();
            JSONObject values = helmChartService.getInstalledValues(sourceName, namespace, clusterDTO);
            if (values != null) {
                recordGroup.setImagePath(getImagePath(values));
                recordGroup.setSourceStatus(MiddlewareStatusEnum.RUNNING.getStatus());
            } else {
                recordGroup.setSourceStatus(MiddlewareStatusEnum.DELETED.getStatus());
            }
        });
    }

    /**
     * 获取指定版本chart包的图片路径
     * @param values
     * @return
     */
    private String getImagePath(JSONObject values) {
        String chartName = values.getString("chart_name");
        String chartVersion = values.getString("chart_version");
        BeanMiddlewareInfo beanMiddlewareInfo = middlewareInfoService.get(chartName, chartVersion);
        if (beanMiddlewareInfo != null) {
            return beanMiddlewareInfo.getImagePath();
        }
        return "";
    }

    /**
     * 设置备份地址
     */
    private void setBackupPosition(List<MiddlewareBackupRecord> records) {
        for (MiddlewareBackupRecord record : records) {
            if (StringUtils.isEmpty(record.getPositionId())) {
                continue;
            }
            String positionId = record.getPositionId();
            BeanBackupPosition backupPosition = backupPositionService.getBackupPosition(Integer.parseInt(positionId));
            if (backupPosition == null) {
                continue;
            }
            BeanBackupServer beanBackupServer = backupServerService.get(backupPosition.getBackupServerId());
            if (beanBackupServer != null) {
                record.setPosition(beanBackupServer.getName() + " - " + backupPosition.getName() + record.getPosition());
            }
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
        backupLabel.put("positionId", backupDTO.getBackupPositionId().toString());
        backupLabel.put("type", backupDTO.getType());
        backupLabel.put("unit", backupDTO.getDateUnit());
        backupDTO.setLabels(backupLabel);
        Map<String,String> annotations = new HashMap<>();
        annotations.put("taskName", backupDTO.getTaskName());
        backupDTO.setAnnotations(annotations);
        backupDTO.setCrdType(middlewareCrdType);
    }

    /**
     * 查询备份记录(立即备份)
     */
    private List<MiddlewareBackupCR> getBackupRecordList(String clusterId, String namespace, String middlewareName,
        String type) {
        List<MiddlewareBackupCR> resList = new ArrayList<>();
        // 查询所有即时备份创建的备份记录
        if (StringUtils.isEmpty(middlewareName) && StringUtils.isEmpty(type)) {
            resList.addAll(backupCRDService.list(clusterId, namespace));
        } else {
            Map<String, String> labels = new HashMap<>();
            labels.put("middleware", getRealMiddlewareName(type, middlewareName));
            resList.addAll(backupCRDService.list(clusterId, namespace, labels));
        }
        return resList;
    }

    private Middleware convertBackupToMiddleware(MiddlewareBackupDTO backupDTO) {
        return new Middleware().setClusterId(backupDTO.getClusterId()).setNamespace(backupDTO.getNamespace())
            .setType(backupDTO.getType()).setName(backupDTO.getMiddlewareName());
    }

    /**
     * 保存备份任务名称到数据库
     * @param backupDTO
     */
    private void saveBackupName(MiddlewareBackupDTO backupDTO) {
        String backupType;
        if (StringUtils.isEmpty(backupDTO.getCron())) {
            backupType = "normal";
        } else {
            backupType = "schedule";
        }
        saveBackupName(backupDTO.getClusterId(), backupDTO.getTaskName(), backupDTO.getLabels().get("backupId"),
                backupType, backupDTO.getBackupPositionId());
    }

    /**
     * 校验是否为增量备份
     */
    public boolean isIncBackup(MiddlewareBackupScheduleCR middlewareBackupScheduleCR){
        AtomicBoolean isIncBackup = new AtomicBoolean(false);
        middlewareBackupScheduleCR.getSpec().getCustomBackups().forEach(cus -> {
            if (cus.containsKey(ENV)){
                cus.get(ENV).forEach(env -> {
                    if (env.containsKey(VALUE) && env.get(VALUE).equals(BACKUP_INC)){
                        isIncBackup.set(true);
                    }
                });
            }
        });
        return isIncBackup.get();
    }

    /**
     * 对象封装: MiddlewareBackupScheduleCR -> MiddlewareBackupRecord
     */
    public void convertBackupScheduleToRecord(MiddlewareBackupScheduleCR schedule, MiddlewareBackupRecord backupRecord){
        if (schedule == null) {
            throw new BusinessException(ErrorMessage.BACKUP_NOT_EXISTS);
        }
        MiddlewareBackupScheduleStatus backupStatus = schedule.getStatus();
        // 获取备份创建时间
        Date creationTime = DateUtils.parseUTCDate(schedule.getMetadata().getCreationTimestamp());
        backupRecord.setCreationTime(creationTime);
        // 获取最近一次备份时间
        backupRecord.setNamespace(schedule.getMetadata().getNamespace());
        backupRecord.setBackupName(schedule.getMetadata().getName());
        backupRecord.setSchedule(true);
        // 获取备份地址id
        backupRecord.setPositionId(schedule.getMetadata().getLabels().get("positionId"));
        // 获取备份位置
        MiddlewareBackupScheduleSpec spec = schedule.getSpec();
        MiddlewareBackupScheduleSpec.MiddlewareBackupScheduleDestination.MiddlewareBackupParameters parameters =
                spec.getBackupDestination().getParameters();
        String position = "(" + parameters.getUrl() + "/"
                + parameters.getBucket() + ")";
        backupRecord.setPosition(position);
        // 获取备份状态
        if (StringUtils.isNotEmpty(schedule.getMetadata().getDeletionTimestamp())){
            backupRecord.setPhrase("Deleting");
        } else if (!ObjectUtils.isEmpty(backupStatus)) {
            backupRecord.setPhrase(backupStatus.getPhase());
        } else {
            backupRecord.setPhrase("Unknown");
        }
        backupRecord.setSourceName(schedule.getSpec().getName());
        backupRecord.setSourceType(middlewareCrTypeService.findTypeByCrType(spec.getType()));
        // 获取labels参数
        Map<String, String> labels = schedule.getMetadata().getLabels();
        Map<String, String> annotations = schedule.getMetadata().getLabels();
        if (!CollectionUtils.isEmpty(labels)){
            backupRecord.setDateUnit(labels.get("unit"));
            backupRecord.setAddressId(labels.get("addressId"));
            backupRecord.setBackupId(labels.get("backupId"));
            backupRecord.setActiveArea(labels.get("activeArea"));
        }
        if (!CollectionUtils.isEmpty(schedule.getMetadata().getAnnotations())) {
            annotations.putAll(schedule.getMetadata().getAnnotations());
        }
        backupRecord.setTaskName(annotations.getOrDefault("taskName", ""));
        // 转换cron表达式
        try {
            backupRecord.setCron(CronUtils.parseCron(schedule.getSpec().getSchedule().getCron(), -timezone + 8));
        }catch (Exception e){
            log.error("定时备份{} 转换cron表达式失败", backupRecord.getBackupName());
            return;
        }
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
    }

    /**
     * 对象封装: MiddlewareBackupCR -> MiddlewareBackupRecord
     */
    public void convertBackupToRecord(MiddlewareBackupCR backup, MiddlewareBackupRecord backupRecord) {
        if (backup == null) {
            throw new BusinessException(ErrorMessage.BACKUP_NOT_EXISTS);
        }
        MiddlewareBackupStatus backupStatus = backup.getStatus();
        Map<String, String> labels = new HashMap<>();
        Map<String, String> annotations = new HashMap<>();
        if (!CollectionUtils.isEmpty(backup.getMetadata().getLabels())) {
            labels.putAll(backup.getMetadata().getLabels());
        }
        if (!CollectionUtils.isEmpty(backup.getMetadata().getAnnotations())) {
            annotations.putAll(backup.getMetadata().getAnnotations());
        }
        backupRecord.setTaskName(annotations.getOrDefault("taskName", ""));

        // 获取备份id
        backupRecord.setBackupId(labels.get("backupId"));

        // 获取备份地址id
        String positionId = StringUtils.isNotEmpty(labels.get("positionId")) ? labels.get("positionId") : labels.get("addressId");
        backupRecord.setPositionId(positionId);

        // 获取备份时间
        Date creationTime = DateUtils.parseUTCDate(backup.getMetadata().getCreationTimestamp());
        backupRecord.setCreationTime(creationTime);
        // 立即备份 备份时间使用创建时间
        backupRecord.setBackupTime(creationTime);
        backupRecord.setNamespace(backup.getMetadata().getNamespace());
        backupRecord.setBackupName(backup.getMetadata().getName());

        // 获取备份位置
        MiddlewareBackupSpec.MiddlewareBackupDestination.MiddlewareBackupParameters parameters =
            backup.getSpec().getBackupDestination().getParameters();
        String position = "(" + parameters.getUrl() + "/"
            + parameters.getBucket() + ")";
        backupRecord.setPosition(position);

        // 获取备份状态
        if (StringUtils.isNotEmpty(backup.getMetadata().getDeletionTimestamp())) {
            backupRecord.setPhrase("Deleting");
        } else if (!ObjectUtils.isEmpty(backupStatus)) {
            backupRecord.setPhrase(backupStatus.getPhase());
            if ("Failed".equals(backupStatus.getPhase())) {
                backupRecord.setReason(backupStatus.getReason());
            }
        } else {
            backupRecord.setPhrase("Unknown");
        }
        backupRecord.setSourceType(middlewareCrTypeService.findTypeByCrType(backup.getSpec().getType()));
        backupRecord.setAddressId(labels.get("addressId"));
        backupRecord.setSourceName(backup.getSpec().getName());
        backupRecord.setBackupMode("single");
        backupRecord.setSchedule(false);
        backupRecord.setOwner(labels.get(OWNER));
        backupRecord.setActiveArea(labels.get("activeArea"));
    }

    /**
     * 获取增量备份备份时间
     */
    public Date checkTimeExist(MiddlewareBackupScheduleCR schedule){
        String type = middlewareCrTypeService.findTypeByCrType(schedule.getSpec().getType());
        if (schedule.getStatus() != null && schedule.getStatus().getStorageProvider() != null && schedule.getStatus().getStorageProvider().containsKey(type)){
            JSONObject time = schedule.getStatus().getStorageProvider().getJSONObject(type);
            return DateUtils.parseUTCDate(time.getString("endTime"));
        }
        return null;
    }

    /**
     * 获取备份时间
     */
    public void setBackupScheduleBackupTime(List<MiddlewareBackupRecord> backupSchedules,
        List<MiddlewareBackupRecord> backupRecords) {
        // 获取由定时备份任务创建出来的备份任务
        List<MiddlewareBackupRecord> backupWithOwner = backupRecords.stream()
            .filter(record -> StringUtils.isNotEmpty(record.getOwner())).collect(Collectors.toList());
        // 转换为map
        Map<String, List<MiddlewareBackupRecord>> backupWithOwnerMap =
            backupWithOwner.stream().collect(Collectors.groupingBy(MiddlewareBackupRecord::getOwner));
        // 根据是否存在备份记录，设置备份时间
        for (MiddlewareBackupRecord scheduleRecord : backupSchedules) {
            if (backupWithOwnerMap.containsKey(scheduleRecord.getBackupName())) {
                List<MiddlewareBackupRecord> recordList = backupWithOwnerMap.get(scheduleRecord.getBackupName());
                // 根据时间降序
                recordList.sort((o1, o2) -> o1.getBackupTime() == null ? -1
                    : o2.getBackupTime() == null ? -1 : o2.getBackupTime().compareTo(o1.getBackupTime()));

                Date backupTime = recordList.get(0).getBackupTime();
                if (backupTime != null) {
                    // 若已设置增量备份时间，与其进行比较
                    if (scheduleRecord.getBackupTime() == null
                        || scheduleRecord.getBackupTime().compareTo(backupTime) < 0) {
                        scheduleRecord.setBackupTime(backupTime);
                    }
                }
            }
        }
    }

    /**
     * 创建备份恢复等待中间件创建完毕
     */
    public Boolean waitingMiddleware(String clusterId, String namespace, String name, String type) {
        boolean flag = false;
        for (int i = 0; i < 20; ++i) {
            MiddlewareCR middlewareCR = null;
            try {
                middlewareCR = middlewareCRService.getCR(clusterId, namespace, type, name);
            } catch (Exception e) {
                log.error("备份恢复 集群{} 分区{} 中间件{} 查询cr状态失败,30s后重试", clusterId, namespace, name);
            }
            if (middlewareCR != null && middlewareCR.getStatus() != null
                && StringUtils.isNotEmpty(middlewareCR.getStatus().getPhase())
                && middlewareCR.getStatus().getPhase().equalsIgnoreCase(RUNNING)) {
                flag = true;
                break;
            }
            try {
                Thread.sleep(30000);
            } catch (Exception ignore) {
            }
        }
        return flag;
    }

    /**
     * 校验备份任务是否已存在
     * @param backupDTO
     */
    public void checkBackupScheduleExist(MiddlewareBackupDTO backupDTO) {
        String backupId = backupDTO.getLabels().get("backupId");
        if (checkBackupScheduleExist(backupDTO.getClusterId(),  backupDTO.getNamespace(), backupDTO.getMiddlewareName(), backupId)) {
            throw new BusinessException(ErrorMessage.MIDDLEWARE_BACKUP_SCHEDULE_EXIST);
        }
    }

    /**
     * 校验备份任务是否已存在
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @param backupId
     * @return
     */
    public boolean checkBackupScheduleExist(String clusterId, String namespace, String middlewareName, String backupId) {
        MiddlewareBackupScheduleList list = backupScheduleCRDService.list(clusterId, namespace);
        if (list != null && !CollectionUtils.isEmpty(list.getItems())) {
            List<MiddlewareBackupScheduleCR> items = list.getItems();
            if (StringUtils.isNotEmpty(backupId)) {
                items = items.stream().filter(cr ->
                        !cr.getMetadata().getLabels().getOrDefault("backupId", backupId).equals(backupId)).collect(Collectors.toList());
            }
            return items.stream().anyMatch(item -> item.getSpec().getName().equals(middlewareName));
        }
        return false;
    }

    /**
     * 根据backupId进行分组
     * @param clusterId
     * @param recordList
     * @return
     */
    public List<MiddlewareBackupRecordGroup> groupByBackupId(String clusterId, List<MiddlewareBackupRecord> recordList) {
        Map<String, List<MiddlewareBackupRecord>> backupIdRecordMap = new HashMap<>();
        for (MiddlewareBackupRecord record : recordList) {
            if (backupIdRecordMap.containsKey(record.getBackupId())) {
                List<MiddlewareBackupRecord> records = backupIdRecordMap.get(record.getBackupId());
                records.add(record);
            } else {
                List<MiddlewareBackupRecord> records = new ArrayList<>();
                records.add(record);
                backupIdRecordMap.put(record.getBackupId(), records);
            }
        }
        List<MiddlewareBackupRecordGroup> recordGroups = new ArrayList<>();
        backupIdRecordMap.forEach((backupId, records) -> {
            MiddlewareBackupRecord record = records.get(0);
            MiddlewareBackupRecordGroup recordGroup = new MiddlewareBackupRecordGroup();
            recordGroup.setMiddlewareBackupRecords(records);
            recordGroup.setBackupMode(record.getBackupMode());
            recordGroup.setClusterId(clusterId);
            recordGroup.setNamespace(record.getNamespace());
            recordGroup.setSourceName(record.getSourceName());
            recordGroup.setSourceType(record.getSourceType());
            recordGroup.setPhrase(getTaskPhrase(records));
            recordGroup.setTaskType(getTaskType(records));
            recordGroup.setBackupId(record.getBackupId());
            BeanMiddlewareBackupName backupName = backupNameService.getByBackupId(record.getBackupId());
            if (backupName != null) {
                recordGroup.setTaskName(backupName.getBackupName());
            } else {
                recordGroup.setTaskName(record.getBackupName());
            }
            recordGroups.add(recordGroup);
        });
        return recordGroups;
    }

    /**
     * 获取备份任务状态
     * @param records
     * @return
     */
    private String getTaskPhrase(List<MiddlewareBackupRecord> records) {
        if (records.size() == 1) {
            return records.get(0).getPhrase();
        }
        String phrase0 = records.get(0).getPhrase();
        String phrase1 = records.get(1).getPhrase();

        if (BackupStatusEnum.RUNNING.getStatus().equals(phrase0) && BackupStatusEnum.RUNNING.getStatus().equals(phrase1)) {
            return BackupStatusEnum.RUNNING.getStatus();
        } else if (BackupStatusEnum.DELETING.getStatus().equals(phrase0) && BackupStatusEnum.DELETING.getStatus().equals(phrase1)) {
            return BackupStatusEnum.DELETING.getStatus();
        } else if (BackupStatusEnum.FAILED.getStatus().equals(phrase0) && BackupStatusEnum.FAILED.getStatus().equals(phrase1)) {
            return BackupStatusEnum.FAILED.getStatus();
        } else if (BackupStatusEnum.CREATING.getStatus().equals(phrase0) && BackupStatusEnum.CREATING.getStatus().equals(phrase1)) {
            return BackupStatusEnum.CREATING.getStatus();
        } else if (BackupStatusEnum.SUCCESS.getStatus().equals(phrase0) && BackupStatusEnum.SUCCESS.getStatus().equals(phrase1)) {
            return BackupStatusEnum.SUCCESS.getStatus();
        } else if (BackupStatusEnum.UNKNOWN.getStatus().equals(phrase0) && BackupStatusEnum.UNKNOWN.getStatus().equals(phrase1)) {
            return BackupStatusEnum.UNKNOWN.getStatus();
        } else {
            return BackupStatusEnum.RUNNING.getStatus();
        }
    }

    /**
     * 获取备份任务类型（1：普通备份，2：双活备份）
     * @param records
     * @return
     */
    private Integer getTaskType(List<MiddlewareBackupRecord> records){
        if (records.size() > 1) {
            return BackupTackTypeEnum.ACTIVE_ACTIVE.getType();
        } else {
            return BackupTackTypeEnum.NORMAL.getType();
        }
    }

    /**
     * 获取可用区选择器annotations
     * @param scheduleAnnotations
     * @return
     */
    private Map<String, String> getAreaSelectorAnnotations(Map<String, String> scheduleAnnotations) {
        Map<String, String> annotations = new HashMap<>();
        if (scheduleAnnotations == null) {
            return annotations;
        }
        if (scheduleAnnotations.containsKey(ActiveAreaConstant.KEY_NODE_SELECTOR)) {
            annotations.put(ActiveAreaConstant.KEY_NODE_SELECTOR, scheduleAnnotations.get(ActiveAreaConstant.KEY_NODE_SELECTOR));
        }
        if (scheduleAnnotations.containsKey(ActiveAreaConstant.KEY_POD_SELECTOR)) {
            annotations.put(ActiveAreaConstant.KEY_POD_SELECTOR, scheduleAnnotations.get(ActiveAreaConstant.KEY_POD_SELECTOR));
        }
        return annotations;
    }

    /**
     * 获取可用区labels
     * @param scheduleLabels
     * @return
     */
    private Map<String, String> getAreaLabels(Map<String, String> scheduleLabels) {
        Map<String, String> labels = new HashMap<>();
        if (scheduleLabels == null) {
            return labels;
        }
        if (scheduleLabels.containsKey("activeArea")) {
            labels.put("activeArea", scheduleLabels.get("activeArea"));
        }
        return labels;
    }

    /**
     * 根据项目过滤
     * @param records
     */
    private List<MiddlewareBackupRecord> filterByProject(List<MiddlewareBackupRecord> records) {
        // 查询用户在当前项目下所有可见的中间件类型
        String username = CurrentUserRepository.getUser().getUsername();
        String projectId = RequestUtil.getProjectId();

        // 根据分区过滤
        List<Namespace> namespaces = projectService.getNamespace(projectId);
        if (!CollectionUtils.isEmpty(namespaces)) {
            Set<String> namespaceSet = namespaces.stream().map(Namespace::getName).collect(Collectors.toSet());
            records = records.stream().filter(record -> namespaceSet.contains(record.getNamespace())).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
        BeanUserRole beanUserRole = userRoleService.get(username, projectId);
        // 超级管理员在项目下没有角色，所以只有当用户为非超级管理员时才按中间件类型过滤
        if (beanUserRole != null) {
            // 根据用户拥有运维权限当中间件类型类型过滤
            Set<String> middlewareSet = roleAuthorityService.listOpsMiddleware(beanUserRole.getRoleId());
            if (!CollectionUtils.isEmpty(middlewareSet)) {
                records = records.stream().filter(record -> middlewareSet.contains(record.getSourceType())).collect(Collectors.toList());
            } else {
                records = Collections.emptyList();
            }
        }
        return records;
    }

    /**
     * 添加备份任务其他信息
     * @param clusterId
     * @param recordList
     * @return
     */
    private List<MiddlewareBackupRecord> addOtherBackupInfo(String clusterId, List<MiddlewareBackupRecord> recordList) {
        // 设置备份任务可用区别名
        setAreaAliasName(clusterId, recordList);
        // 获取任务对应的中文名称
        setTaskName(recordList, clusterId, null);
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

}
