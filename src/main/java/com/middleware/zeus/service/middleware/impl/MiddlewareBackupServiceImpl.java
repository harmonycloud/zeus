package com.middleware.zeus.service.middleware.impl;

import static com.middleware.zeus.common.constants.BackupConstant.*;
import static com.middleware.zeus.common.constants.CommonConstant.INCR;
import static com.middleware.zeus.common.constants.NameConstant.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.constants.ActiveAreaConstant;
import com.middleware.zeus.common.model.user.UserRole;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.zeus.util.RequestUtil;
import com.middleware.zeus.util.date.DateUtils;
import com.middleware.zeus.util.numeric.MemoryUnitEnum;
import com.middleware.zeus.util.numeric.ResourceCalculationUtil;
import com.middleware.zeus.bean.*;
import com.middleware.zeus.common.enums.*;
import com.middleware.zeus.common.model.*;
import com.middleware.zeus.common.model.middleware.*;
import com.middleware.zeus.dao.BeanCustomConfigMapper;
import com.middleware.zeus.integration.cluster.DeploymenentWrapper;
import com.middleware.zeus.integration.cluster.StatefulSetWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareBackup;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.service.middleware.*;
import com.middleware.zeus.integration.cluster.bean.*;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.service.user.RoleAuthorityService;
import com.middleware.zeus.service.user.UserService;
import com.middleware.zeus.util.numeric.MathUtil;
import com.middleware.zeus.util.middleware.MiddlewareBackupTrimUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.util.uuid.UUIDUtils;
import com.middleware.zeus.dao.BeanMiddlewareBackupNameMapper;
import com.middleware.zeus.util.CronUtils;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;

/**
 * 中间件通用备份
 *
 * @author liyinlong
 * @since 2021/9/15 3:22 下午
 */
@Slf4j
@com.middleware.zeus.annotation.MiddlewareBackup
@Service
public class MiddlewareBackupServiceImpl implements MiddlewareBackupService {

    @Value("${system.cron.timezone: 0}")
    private Integer timezone;

    @Value("${system.componentNamespace:middleware-operator}")
    private String componentNamespace;

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
    private MiddlewareServiceImpl middlewareService;
    @Autowired
    private ActiveAreaService activeAreaService;
    @Autowired
    private BackupServerService backupServerService;
    @Autowired
    private RoleAuthorityService roleAuthorityService;
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
    @Autowired
    private UserService userService;
    @Autowired
    private PodService podService;
    @Autowired
    private BeanCustomConfigMapper beanCustomConfigMapper;
    @Autowired
    private StatefulSetWrapper statefulSetWrapper;
    @Autowired
    private DeploymenentWrapper deploymenentWrapper;

    // <可用区英文名,可用区别名>
    private static final Map<String, String> activeAreaMap = new HashMap<>();

    @Override
    public List<MiddlewareBackupRecord> listBackup(String clusterId, String namespace, String middlewareName,
                                                   String type) {
        List<MiddlewareBackupRecord> recordList = new ArrayList<>();
        // 查询通用备份列表
        List<MiddlewareBackup> backupRecordList = getBackupRecordList(clusterId, namespace, middlewareName, type);
        if (!CollectionUtils.isEmpty(backupRecordList)) {
            for (MiddlewareBackup item : backupRecordList) {
                MiddlewareBackupRecord backupRecord = new MiddlewareBackupRecord();
                convertBackupToRecord(clusterId, item, backupRecord);
                recordList.add(backupRecord);
            }
        }
        // 查询mysql备份列表(旧)
        recordList.addAll(mysqlAdapterService.listBackup(clusterId, namespace, middlewareName, type));
        return recordList;
    }

    @Override
    public List<MiddlewareBackupRecord> getBackup(String clusterId, String namespace, String backupId, String backupMode) {
        List<MiddlewareBackupRecord> records;
        if (BackupMode.PERIOD.getMode().equals(backupMode)) {
            List<MiddlewareBackupSchedule> scheduleCRList = listMiddlewareBackupSchedule(clusterId, namespace, backupId);
            records = convertBackupSchedulesToRecords(clusterId, scheduleCRList);
            MiddlewareBackupTrimUtil.trimScheduleBackup(records);
        } else {
            List<MiddlewareBackup> backupCRList = listMiddlewareBackup(clusterId, namespace, backupId);
            records = convertBackupsToRecords(clusterId, backupCRList);
            records.forEach(middlewareBackupRecord -> {
                middlewareBackupRecord.setSameActiveActiveBackup(true);
            });
        }
        setBackupPosition(records);
        return records;
    }

    @Override
    public void createBackup(MiddlewareBackupDTO backupDTO) {
        if (backupDTO.getIncrement() != null && backupDTO.getIncrement() && backupDTO.getIncrement() && "day".equalsIgnoreCase(backupDTO.getDateUnit())) {
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
    public void createOrReplaceIncBackup(String clusterId, String namespace, String backupId, String backupName, String time, String pause, Boolean sameActiveActiveBackup) {
        if (sameActiveActiveBackup) {
            List<MiddlewareBackupSchedule> schedules = listMiddlewareBackupSchedule(clusterId, namespace, backupId);
            for (MiddlewareBackupSchedule backupSchedule : schedules) {
                createOrReplaceIncBackup(clusterId, namespace, backupSchedule.getMetadata().getName(), time, pause);
            }
        } else {
            createOrReplaceIncBackup(clusterId, namespace, backupName, time, pause);
        }
    }

    @Override
    public void createOrReplaceIncBackup(String clusterId, String namespace, String backupName, String time, String pause) {
        // 校验备份周期和保留时间
        MiddlewareBackupSchedule baks = backupScheduleCRDService.get(clusterId, namespace, backupName);
        if (baks == null || baks.getSpec() == null || baks.getSpec().getSchedule() == null) {
            throw new BusinessException(ErrorMessage.FIND_BACKUP_SCHEDULE_CRON_FAILED);
        }
        String cron = baks.getSpec().getSchedule().getCron();
        Integer retentionTime = baks.getSpec().getSchedule().getRetentionTime();
        if ("day".equalsIgnoreCase(baks.getMetadata().getLabels().get("unit"))) {
            checkTimeLawful(cron, retentionTime);
        }
        createOrReplaceIncBackup(clusterId, namespace, backupName, time, pause, baks);
    }

    @Override
    public void createOrReplaceIncBackup(String clusterId, String namespace, String backupName, String time, String pause, MiddlewareBackupSchedule scheduleCR) {
        MiddlewareIncBackup incBackup = new MiddlewareIncBackup();
        incBackup.setClusterId(clusterId);
        incBackup.setNamespace(namespace);
        incBackup.setBackupName(backupName);
        incBackup.setTime(time);
        incBackup.setPause(pause);
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
        createOrReplaceIncBackupSchedule(incBackup, meta);
    }

    @Override
    public void updateBackupSchedule(MiddlewareBackupDTO backupDTO) {
        if (backupDTO.getSameActiveActiveBackup()) {
            List<MiddlewareBackupSchedule> schedules = listMiddlewareBackupSchedule(backupDTO.getClusterId(), backupDTO.getNamespace(), backupDTO.getBackupId());
            for (MiddlewareBackupSchedule schedule : schedules) {
                updateBackupSchedule(schedule.getMetadata().getName(), backupDTO);
            }
        } else {
            updateBackupSchedule(backupDTO.getBackupName(), backupDTO);
        }
    }

    @Override
    public void updateBackupSchedule(String backupName, MiddlewareBackupDTO backupDTO) {
        backupDTO.setBackupName(backupName);
        if ("day".equalsIgnoreCase(backupDTO.getDateUnit()) && backupDTO.getIncrement() != null && backupDTO.getIncrement()) {
            checkTimeLawful(backupDTO.getCron(), backupDTO.getRetentionTime());
        }
        // 是否为mysqlBackup
        if (backupDTO.getMysqlBackup() != null && backupDTO.getMysqlBackup()) {
            mysqlAdapterService.updateBackupSchedule(backupDTO);
        } else {
            MiddlewareBackupSchedule middlewareBackupSchedule = backupScheduleCRDService
                    .get(backupDTO.getClusterId(), backupDTO.getNamespace(), backupDTO.getBackupName());
            MiddlewareBackupScheduleSpec spec = middlewareBackupSchedule.getSpec();
            // 更新cron表达式
            if (StringUtils.isNotEmpty(backupDTO.getCron())) {
                spec.getSchedule().setCron(CronUtils.parseCron(backupDTO.getCron(), -8 + timezone));
            }
            // 更新备份保留时间
            if (backupDTO.getRetentionTime() != null && StringUtils.isNotEmpty(backupDTO.getDateUnit())) {
                spec.getSchedule().setRetentionTime(calRetentionTime(backupDTO));
                middlewareBackupSchedule.getMetadata().getLabels().put("unit", backupDTO.getDateUnit());
            }
            try {
                backupScheduleCRDService.update(backupDTO.getClusterId(), middlewareBackupSchedule);
            } catch (IOException e) {
                log.error("中间件{}备份设置更新失败", backupDTO.getMiddlewareName());
                throw new BusinessException(ErrorMessage.MIDDLEWARE_BACKUP_UPDATE_FAILED);
            }
            // 增量备份更新
            if (backupDTO.getIncrement() != null && backupDTO.getIncrement()) {
                MiddlewareBackupSchedule incBackupScheduleCr = backupScheduleCRDService.get(backupDTO.getClusterId(),
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
    public void deleteRecord(String clusterId, String namespace, String type, String backupName, Boolean forceDelete) {
        try {
            backupCRDService.delete(clusterId, namespace, backupName, forceDelete);
        } catch (Exception e) {
            if (MiddlewareTypeEnum.MYSQL.getType().equals(type)) {
                mysqlAdapterService.deleteRecord(clusterId, namespace, type, backupName, forceDelete);
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
        MiddlewareBackupSchedule crd = new MiddlewareBackupSchedule();
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
            createOrReplaceIncBackup(backupDTO.getClusterId(), backupDTO.getNamespace(), meta.getName(), backupDTO.getTime(), "off", crd);
        }
    }

    /**
     * 创建通用备份
     *
     * @param backupDTO
     * @param minio
     * @param objectMeta
     */
    @Override
    public void createNormalBackup(MiddlewareBackupDTO backupDTO, Minio minio, ObjectMeta objectMeta) {
        MiddlewareBackup middlewareBackup = new MiddlewareBackup();
        ObjectMeta meta = getMiddlewareBackupMeta(backupDTO, objectMeta);
        middlewareBackup.setMetadata(meta);
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
        middlewareBackup.setSpec(spec);
        try {
            backupCRDService.create(backupDTO.getClusterId(), middlewareBackup);
        } catch (IOException e) {
            log.error("立即备份失败", e);
        }
    }

    /**
     * 创建增量备份
     *
     * @param middlewareIncBackup
     * @param objectMeta
     */
    @Override
    public void createOrReplaceIncBackupSchedule(MiddlewareIncBackup middlewareIncBackup, ObjectMeta objectMeta) {
        String clusterId = middlewareIncBackup.getClusterId();
        String namespace = middlewareIncBackup.getNamespace();
        String backupName = middlewareIncBackup.getBackupName();
        String time = middlewareIncBackup.getTime();
        String pause = middlewareIncBackup.getPause();
        if (StringUtils.isEmpty(pause)) {
            pause = "on";
        }
        Map<String, String> annotations = middlewareIncBackup.getAnnotations();
        MiddlewareBackupSchedule cr = backupScheduleCRDService.get(clusterId, namespace, backupName);
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
        backupLabel.put("owner", backupName);

        cr.setMetadata(objectMeta);
        cr.setStatus(null);
        // 设置增量备份
        cr.getSpec().getCustomBackups().forEach(cus -> {
            if (cus.containsKey(ENV)) {
                cus.get(ENV).forEach(env -> {
                    if (env.containsKey(VALUE) && env.get(VALUE).equals(BACKUP)) {
                        env.put(VALUE, BACKUP_INC);
                    }
                });
            }
        });
        // 设置增量备份开关
        cr.getSpec().setPause(pause);
        // 转换时间单位
        cr.getSpec().getSchedule().setCron(CronUtils.convertTimeToCron(time));
        try {
            backupScheduleCRDService.createOrReplace(clusterId, cr);
        } catch (Exception e) {
            log.error("集群{}分区{}创建增量备份{}失败", clusterId, namespace, backupName + "-incr", e);
            throw new BusinessException(ErrorMessage.CREATE_INCREMENT_BACKUP_FAILED);
        }
    }

    /**
     * 根据备份任务类型（是否双活）创建备份任务
     *
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
     *
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
     *
     * @param activeAreaAnnotationDto
     * @param serverUsage             A：可用区A，B：可用区B
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
     *
     * @param backupDTO
     * @return
     */
    private boolean activeActiveBackupCheck(MiddlewareBackupDTO backupDTO) {
        String clusterId = backupDTO.getClusterId();
        String namespace = backupDTO.getNamespace();
        String middlewareName = backupDTO.getMiddlewareName();
        boolean activeActiveNamespace = namespaceService.isOpenAvailableDomain(clusterId, namespace);
        BeanBackupServer backupServer = backupPositionService.getBackupServer(backupDTO.getBackupPositionId());

        if (activeActiveNamespace && (backupServer.getType() == 2) && checkActiveActiveMiddleware(clusterId, namespace, middlewareName, backupDTO.getType())) {
            String type = backupDTO.getType();
            return type.equals(MiddlewareTypeEnum.MYSQL.getType()) || type.equals(MiddlewareTypeEnum.POSTGRESQL.getType()) || type.equals(MiddlewareTypeEnum.REDIS.getType());
        }
        return false;
    }

    /**
     * 检查中间件是否是双活中间件，双活中间件：所有pod所在节点都是可用区
     *
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @return
     */
    private boolean checkActiveActiveMiddleware(String clusterId, String namespace, String middlewareName, String type) {
        List<PodInfo> podInfos = podService.listMiddlewarePodsWithArea(clusterId, namespace, middlewareName, type);
        for (PodInfo podInfo : podInfos) {
            if (StringUtils.isEmpty(podInfo.getNodeZone())) {
                return false;
            }
        }
        return true;
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
     *
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
    public void createRestore(MiddlewareRestoreDto restoreDto) {
        String clusterId = restoreDto.getClusterId();
        String namespace = restoreDto.getNamespace();
        String type = restoreDto.getType();
        String middlewareName = restoreDto.getMiddlewareName();
        String backupName = restoreDto.getBackupName();
        String sourceName = restoreDto.getSourceName();
        String backupId = restoreDto.getBackupId();
        String activeArea = restoreDto.getActiveArea();
        String restoreTime = restoreDto.getRestoreTime();

        if (StringUtils.isAnyEmpty(clusterId, namespace, type, middlewareName, backupName, sourceName, backupId)) {
            throw new BusinessException(ErrorMessage.PARAMETER_NOT_COMPLETE);
        }
        MiddlewareRestoreCR crd = new MiddlewareRestoreCR();
        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace(namespace);
        meta.setName(middlewareName + "-restore-" + UUIDUtils.get8UUID());
        // 设置对应label
        Map<String, String> backupLabel = getBackupLabel(sourceName, type);
        backupLabel.put("backupId", backupId);
        backupLabel.put("activeArea", activeArea);
        backupLabel.put("sourceName", sourceName);
        meta.setLabels(backupLabel);
        crd.setMetadata(meta);

        MiddlewareRestoreSpec spec = new MiddlewareRestoreSpec();
        List<String> args = new ArrayList<>();
        List<Map<String, String>> envList = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        args.add("--backupNamespace=" + namespace);
        args.add("--backupResultName=" + backupName);
        if (StringUtils.isEmpty(restoreTime)) {
            if (MiddlewareTypeEnum.POSTGRESQL.getType().equals(type) || MiddlewareTypeEnum.MYSQL.getType().equals(type)) {
                args.add("--mode=full");
            }
        } else {
            args.add("--mode=inc");
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
        } catch (Exception e) {
            log.error("集群{} 中间件{} 克隆实例失败", clusterId, middlewareName, e);
            throw new BusinessException(ErrorMessage.BACKUP_RESTORE_FAILED);
        }
    }

    @Override
    public void deleteMiddlewareBackupInfo(String clusterId, String namespace, String type, String middlewareName) {
        Map<String, String> labels = getBackupLabel(middlewareName, type);
        // 删除定时备份
        List<MiddlewareBackupSchedule> middlewareBackupScheduleList =
                backupScheduleCRDService.listByLabels(clusterId, namespace, labels);
        middlewareBackupScheduleList.forEach(item -> {
            try {
                backupScheduleCRDService.delete(clusterId, namespace, item.getMetadata().getName());
            } catch (IOException e) {
                log.error("删除定时备份失败");
            }
        });
        // 删除立即备份
        List<MiddlewareBackup> backupCRList = backupCRDService.list(clusterId, namespace, labels);
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
        Map<String, MiddlewareBackupSchedule> incBackup = new HashMap<>();
        for (MiddlewareBackupSchedule schedule : scheduleList.getItems()) {
            // 处理增量备份数据
            if (isIncBackup(schedule)) {
                incBackup.put(schedule.getMetadata().getName(), schedule);
                continue;
            }

            MiddlewareBackupRecord backupRecord = new MiddlewareBackupRecord();
            convertBackupScheduleToRecord(clusterId, schedule, backupRecord);
            recordList.add(backupRecord);
        }
        // 设置增量备份
        recordList.forEach(record -> {
            if (incBackup.containsKey(record.getBackupName() + "-" + INCR)) {
                record.setIncrement(true);
                MiddlewareBackupSchedule incSchedule = incBackup.get(record.getBackupName() + "-" + INCR);
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
            } catch (Exception ignored) {
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
     *
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
        // 过滤backupSchedule所产生的backup  过滤删除中的任务
        backupRecords = backupRecords.stream().filter(backupRecord -> StringUtils.isEmpty(backupRecord.getOwner()))
                .filter(backupRecord -> StringUtils.isEmpty(backupRecord.getPhrase())
                        || !"Deleting".equals(backupRecord.getPhrase()))
                .collect(Collectors.toList());

        recordList.addAll(backupRecords);
        recordList.addAll(backupSchedules);
        // 设置备份任务可用区别名
        setAreaAliasName(clusterId, recordList);
        // 获取任务对应的中文名称
        setTaskName(recordList, clusterId, null);
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
    public List<MiddlewareBackupRecordGroup> backupTaskGroupList(String clusterId, String namespace, String middlewareName, String organId, String projectId, String type, String keyword) {
        List<MiddlewareBackupRecord> records = backupTaskList(clusterId, namespace, middlewareName, type, keyword);
        // 根据项目过滤中间件
        if (StringUtils.isNotEmpty(projectId)) {
            records = filterByProject(records, organId, projectId);
        }
        // 设置备份地址
        setBackupPosition(records);
        List<MiddlewareBackupRecordGroup> recordGroups = groupByBackupId(clusterId, records);
        setMiddlewareStatus(clusterId, recordGroups);
        return recordGroups;
    }

    @Override
    public MiddlewareIncBackupDto getIncBackupInfo(String clusterId, String namespace, String backupName) {
        MiddlewareBackupSchedule owner = backupScheduleCRDService.get(clusterId, namespace, backupName);
        MiddlewareBackupSchedule cr = backupScheduleCRDService.get(clusterId, namespace, backupName + "-incr");
        if (cr == null) {
            return null;
        }
        MiddlewareIncBackupDto middlewareIncBackupDto = new MiddlewareIncBackupDto();
        middlewareIncBackupDto.setBackupName(backupName + "-incr");
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

        // 设置可用区信息
        if (cr.getMetadata() != null && cr.getMetadata().getLabels() != null && cr.getMetadata().getLabels().containsKey("activeArea")) {
            String activeArea = cr.getMetadata().getLabels().get("activeArea");
            middlewareIncBackupDto.setActiveArea(activeArea);
            middlewareIncBackupDto.setAreaAliasName(getActiveAreaAliasName(clusterId, activeArea));
        }
        if (owner != null && owner.getMetadata() != null && owner.getMetadata().getLabels() != null && owner.getMetadata().getLabels().containsKey("backupId")) {
            middlewareIncBackupDto.setBackupId(owner.getMetadata().getLabels().get("backupId"));
        }
        // 封装数据
        middlewareIncBackupDto.setPause(cr.getSpec().getPause())
                .setTime(CronUtils.convertCronToTime(cr.getSpec().getSchedule().getCron()))
                .setBackupName(cr.getMetadata().getName());
        return middlewareIncBackupDto;
    }

    @Override
    public List<MiddlewareIncBackupDto> getIncBackupInfoList(String clusterId, String namespace, String backupId) {
        List<MiddlewareBackupSchedule> scheduleCRList = listMiddlewareBackupSchedule(clusterId, namespace, backupId);
        List<MiddlewareIncBackupDto> incBackupDtos = new ArrayList<>();
        if (scheduleCRList.size() == 2) {
            MiddlewareBackupSchedule scheduleA = scheduleCRList.get(0);
            MiddlewareBackupSchedule scheduleB = scheduleCRList.get(1);
            MiddlewareIncBackupDto incBackupInfoA = getIncBackupInfo(clusterId, namespace, scheduleA.getMetadata().getName());
            MiddlewareIncBackupDto incBackupInfoB = getIncBackupInfo(clusterId, namespace, scheduleB.getMetadata().getName());
            if (incBackupInfoA == null || incBackupInfoB == null) {
                return Collections.emptyList();
            }
            if (incBackupInfoA.getTime().equals(incBackupInfoB.getTime())
                    && incBackupInfoA.getPause().equals(incBackupInfoB.getPause())) {
                incBackupInfoA.setSameActiveActiveBackup(true);
                incBackupDtos.add(incBackupInfoA);
                incBackupDtos.add(incBackupInfoB);
            } else {
                incBackupInfoA.setSameActiveActiveBackup(true);
                incBackupInfoB.setSameActiveActiveBackup(true);
                incBackupDtos.add(incBackupInfoA);
                incBackupDtos.add(incBackupInfoB);
            }
        } else {
            for (MiddlewareBackupSchedule schedule : scheduleCRList) {
                MiddlewareIncBackupDto incBackupInfo = getIncBackupInfo(clusterId, namespace, schedule.getMetadata().getName());
                if (incBackupInfo != null) {
                    incBackupDtos.add(incBackupInfo);
                }
            }
        }
        return incBackupDtos;
    }

    @Override
    public List<MiddlewareBackupRecord> backupRecords(String clusterId, String namespace, String middlewareName, String type,
                                                      String backupId, String backupMode, String orderBy, String activeArea) {
        // 获取所有备份记录：包含单次备份、周期备份定时创建的、增量备份定时创建的
        List<MiddlewareBackupRecord> recordList = listBackup(clusterId, namespace, null, null);
        if ("single".equals(backupMode)) {
            recordList = recordList.stream().filter(record -> backupId.equals(record.getBackupId())).collect(Collectors.toList());
        } else {
            // 查询周期备份记录：先查询周期备份任务名字列表，然后根据备份记录的owner过滤周期备份任务定时创建的备份记录
            Map<String, MiddlewareBackupRecord> scheduleNamesMap = listMiddlewareBackupScheduleNamesMap(clusterId, namespace, backupId);
            Set<String> scheduleNames = scheduleNamesMap.keySet();
            recordList = recordList.stream().filter(record -> {
                if (StringUtils.isNotEmpty(record.getOwner()) &&
                        scheduleNames.contains(record.getOwner())) {
                    MiddlewareBackupRecord schedule = scheduleNamesMap.get(record.getOwner());
                    if (schedule != null) {
                        record.setActiveArea(schedule.getActiveArea());
                        record.setAreaAliasName(schedule.getAreaAliasName());
                    }
                    return true;
                }
                return false;
            }).collect(Collectors.toList());
        }
        if (StringUtils.isNotEmpty(activeArea)) {
            recordList = recordList.stream().filter(record ->
                    activeArea.equals(record.getActiveArea())).collect(Collectors.toList());
        }
        setBackupPosition(recordList);
        return sortAndSetAliasName(recordList, orderBy);
    }

    @Override
    public ProgressInfo getBackupProgress(String clusterId, String namespace, String middlewareName, String backupName) {
        // 查询backup cr
        MiddlewareBackup backup = backupCRDService.get(clusterId, namespace, backupName);
        Map<String, String> annotations = backup.getMetadata().getAnnotations();

        ProgressInfo progressInfo = new ProgressInfo();
        if (annotations.containsKey("middleware.maintenance.step")) {
            String currentStep = annotations.get("middleware.maintenance.step");
            int currentStepNum = (Integer.parseInt(currentStep) + 1);
            String stepDescription = currentStepNum + "/3 " + BackupStepEnum.findStepDescriptionByStep(annotations.get("middleware.maintenance.step.str"));
            progressInfo.setProgressDescription(stepDescription);
            Float currentProgress = currentStepNum / 3f;
            progressInfo.setCurrentProgress(currentProgress);
        }
        try {
            Date creationTime = DateUtils.parseUTCDate(backup.getMetadata().getCreationTimestamp());
            progressInfo.setCreateTime(creationTime);
        } catch (Exception e) {
            log.error("设置时间失败", e);
        }
        progressInfo.setClusterId(clusterId);
        progressInfo.setNamespace(namespace);
        progressInfo.setBackupSourceName(middlewareName);
        if (backup.getStatus() != null) {
            progressInfo.setPhrase(backup.getStatus().getPhase());
        }
        // 设置备份存储大小
        setBackupSize(backup.getStatus(), progressInfo, backup.getSpec().getType());
        // 查询backup任务pods
        progressInfo.setTaskPods(getTaskPods(clusterId, namespace, backupName));
        // 查询备份控制器状态
        progressInfo.setBackupControllerStatus(getBackupComponentStatus(clusterId));
        // 设置可用区信息
        setActiveAreaInfo(clusterId, namespace, backup, progressInfo);
        return progressInfo;
    }

    @Override
    public ProgressInfo getRestoreProgress(String clusterId, String namespace, String middlewareName, String restoreName) {
        ProgressInfo progressInfo = new ProgressInfo();
        progressInfo.setClusterId(clusterId);
        progressInfo.setNamespace(namespace);
        progressInfo.setBackupSourceName(middlewareName);
        // 查询restore cr
        MiddlewareRestoreCR restoreCR = restoreCRDService.get(clusterId, namespace, restoreName);
        Map<String, String> annotations = restoreCR.getMetadata().getAnnotations();
        if (annotations.containsKey("middleware.maintenance.step")) {
            String currentStep = annotations.get("middleware.maintenance.step");
            int currentStepNum = (Integer.parseInt(currentStep) + 1);
            String stepDescription = currentStepNum + "/7 " + RestoreStepEnum.findStepDescriptionByStep(annotations.get("middleware.maintenance.step.str"));
            progressInfo.setProgressDescription(stepDescription);
            Float currentProgress = currentStepNum / 7f;
            progressInfo.setCurrentProgress(currentProgress);
        }
        try {
            Date creationTime = DateUtils.parseUTCDate(restoreCR.getMetadata().getCreationTimestamp());
            progressInfo.setCreateTime(creationTime);
        } catch (Exception e) {
            log.error("设置时间失败", e);
        }
        // 查询restore进程pods
        progressInfo.setTaskPods(getTaskPods(clusterId, namespace, restoreName));
        // 查询备份控制器状态
        progressInfo.setBackupControllerStatus(getBackupComponentStatus(clusterId));
        progressInfo.setBackupSourceName(middlewareName);
        return progressInfo;
    }

    @Override
    public List<MiddlewareBackupRecord> backupIncrRecords(String clusterId, String namespace, String middlewareName, String type, String backupId, String backupMode, String orderBy) {
        Map<String, MiddlewareBackupRecord> scheduleNamesMap = listMiddlewareBackupScheduleNamesMap(clusterId, namespace, backupId);
        Set<String> backupScheduleNames = scheduleNamesMap.keySet();
        Set<String> incrScheduleNames = new HashSet<>();
        Map<String, MiddlewareBackupRecord> incrScheduleNamesMap = new HashMap<>();
        backupScheduleNames.forEach(scheduleName -> {
            String incrScheduleName = scheduleName + "-incr";
            MiddlewareBackupSchedule incrSchedule = backupScheduleCRDService.get(clusterId, namespace, incrScheduleName);
            if (incrSchedule != null) {
                incrScheduleNames.add(incrSchedule.getMetadata().getName());
                incrScheduleNamesMap.put(incrScheduleName, scheduleNamesMap.get(scheduleName));
            }
        });
        List<MiddlewareBackupRecord> recordList = listBackup(clusterId, namespace, null, null);
        recordList = recordList.stream().filter(record -> {
            if (StringUtils.isNotEmpty(record.getOwner()) && incrScheduleNames.contains(record.getOwner())) {
                MiddlewareBackupRecord schedule = incrScheduleNamesMap.get(record.getOwner());
                if (schedule != null) {
                    record.setTaskName(schedule.getTaskName());
                    record.setActiveArea(schedule.getActiveArea());
                    record.setAreaAliasName(schedule.getAreaAliasName());
                }
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        return sortAndSetAliasName(recordList, orderBy);
    }

    @Override
    public List<MiddlewareBackupRestore> backupRestores(String clusterId, String namespace, String backupId) {
        Map<String, String> labels = new HashMap<>();
        labels.put("backupId", backupId);
        MiddlewareRestoreList restoreList = restoreCRDService.list(clusterId, namespace, labels);
        if (restoreList == null) {
            return Collections.emptyList();
        }
        List<MiddlewareRestoreCR> restores = restoreList.getItems();
        if (!CollectionUtils.isEmpty(restores)) {
            return convertMiddlewareRestore(restores, clusterId);
        }
        return Collections.emptyList();
    }

    @Override
    public void deleteRestoreRecord(String clusterId, String namespace, String restoreName) {
        try {
            restoreCRDService.delete(clusterId, namespace, restoreName);
        } catch (Exception e) {
            log.error("删除克隆记录失败", e);
            throw new BusinessException(ErrorMessage.FAILED_TO_DELETE_BACKUP_POSITION);
        }
    }

    @Override
    public void deleteRestoreRecord(String clusterId, String namespace, String restoreName, Boolean forceDelete) {
        try {
            restoreCRDService.delete(clusterId, namespace, restoreName, forceDelete);
        } catch (Exception e) {
            log.error("强制删除克隆记录失败", e);
            throw new BusinessException(ErrorMessage.FAILED_TO_DELETE_BACKUP_POSITION);
        }
    }

    @Override
    public void deleteBackUpTask(MiddlewareTaskDTO taskDTO) {
        String clusterId = taskDTO.getClusterId();
        String namespace = taskDTO.getNamespace();
        String type = taskDTO.getType();
        String backupId = taskDTO.getBackupId();
        List<String> backupNameList = taskDTO.getBackupNameList();
        backupNameList.forEach(backupName -> {
            if ("period".equals(taskDTO.getBackupMode())) {
                deleteSchedule(clusterId, namespace, type, backupName);
            } else {
                deleteRecord(clusterId, namespace, type, backupName, false);
            }
        });
        if (StringUtils.isNotEmpty(backupId)) {
            deleteBackupName(clusterId, backupId);
        }
    }

    @Override
    public void deleteBackUpRecord(String clusterId, String namespace, String type, String backupName, String backupId, Boolean forceDelete) {
        // 删除备份cr
        deleteRecord(clusterId, namespace, type, backupName, forceDelete);
        // 删除对应数据库记录
        if (StringUtils.isNotEmpty(backupId)) {
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

    @Override
    public List<MiddlewareBackupRecord> listBackupTask(String clusterId, String namespace, Map<String, String> labels) {
        List<MiddlewareBackupRecord> records = new ArrayList<>();
        List<MiddlewareBackupSchedule> scheduleCRS = backupScheduleCRDService.listByLabels(clusterId, namespace, labels);
        if (!CollectionUtils.isEmpty(scheduleCRS)) {
            for (MiddlewareBackupSchedule scheduleCR : scheduleCRS) {
                MiddlewareBackupRecord record = new MiddlewareBackupRecord();
                convertBackupScheduleToRecord(scheduleCR, record);
                records.add(record);
            }
        }
        List<MiddlewareBackup> backupCRList = backupCRDService.list(clusterId, namespace, labels);
        if (!CollectionUtils.isEmpty(backupCRList)) {
            for (MiddlewareBackup backupCR : backupCRList) {
                MiddlewareBackupRecord record = new MiddlewareBackupRecord();
                convertBackupToRecord(clusterId, backupCR, record);
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
     * 获取备份组件状态
     *
     * @param clusterId
     * @return 1：运行正常，0：运行异常
     */
    private Integer getBackupComponentStatus(String clusterId) {
        Map<String, String> label = new HashMap<>();
        label.put("control-plane", "backup-controller");
        List<PodInfo> podInfos = podService.list(clusterId, componentNamespace, label);
        if (CollectionUtils.isEmpty(podInfos)) {
            podInfos = podService.list(clusterId, componentNamespace).stream().
                    filter(podInfo -> "middlewarebackup-controller".equals(podInfo.getPodName())).collect(Collectors.toList());
        }
        for (PodInfo podInfo : podInfos) {
            if ("Running".equals(podInfo.getStatus())) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * 设置可用区信息
     * @param clusterId
     * @param namespace
     * @param backup
     * @param progressInfo
     */
    private void setActiveAreaInfo(String clusterId, String namespace, MiddlewareBackup backup, ProgressInfo progressInfo) {
        if (backup != null && backup.getMetadata() != null && !CollectionUtils.isEmpty(backup.getMetadata().getLabels())) {
            if (backup.getMetadata().getLabels().containsKey("activeArea")) {
                String activeArea = backup.getMetadata().getLabels().get("activeArea");
                progressInfo.setActiveArea(activeArea);
                progressInfo.setAreaAliasName(getActiveAreaAliasName(clusterId, activeArea));
            } else if (backup.getMetadata().getLabels().containsKey("owner")) {
                String owner = backup.getMetadata().getLabels().get("owner");
                MiddlewareBackupSchedule schedule = backupScheduleCRDService.get(clusterId, namespace, owner);
                if (schedule != null) {
                    String activeArea = schedule.getMetadata().getLabels().get("activeArea");
                    progressInfo.setActiveArea(activeArea);
                    progressInfo.setAreaAliasName(getActiveAreaAliasName(clusterId, activeArea));
                }
            }
        }
    }

    /**
     * 返回备份或恢复任务pod信息
     *
     * @param clusterId
     * @param namespace
     * @param ownerName 备份或恢复的cr name
     * @return
     */
    private List<PodInfo> getTaskPods(String clusterId, String namespace, String ownerName) {
        Map<String, String> labels = new HashMap<>();
        labels.put("owner", ownerName);
        List<PodInfo> podInfos = podService.list(clusterId, namespace, labels);
        podInfos.sort(Comparator.comparing(PodInfo::getPodName));

        for (int i = 0; i < podInfos.size(); i++) {
            podInfos.get(i).setPodAliasName("备份进程" + getLetterByIndex(i));
        }
        return podInfos;
    }

    /**
     * 根据输入的index返回对应的大写字母。
     * index的范围必须在0到25之间(包含0和25)。
     *
     * @param index 要返回字母的索引，范围在0到25之间(包含0和25)
     * @return 返回对应索引的字母
     * @throws IllegalArgumentException 如果输入的index不在有效范围内，将抛出IllegalArgumentException异常
     */
    private char getLetterByIndex(int index) {
        if (index < 0 || index > 25) {
            throw new IllegalArgumentException("Index must be between 0 and 25.");
        }
        char letter = (char) (index + 'A'); // ASCII码中a的值为97
        return letter;
    }

    public void deleteBackupName(String clusterId, String backupId) {
        QueryWrapper<BeanMiddlewareBackupName> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id", clusterId).eq("backup_id", backupId);
        middlewareBackupNameMapper.delete(wrapper);
    }

    /**
     * 对克隆记录进行排序
     * @param restoreList
     */
    private void sortMiddlewareRestore(List<MiddlewareBackupRestore> restoreList) {
        // 创建一个Comparator对象
        Comparator<MiddlewareBackupRestore> comparator = new Comparator<MiddlewareBackupRestore>() {
            @Override
            public int compare(MiddlewareBackupRestore restore1, MiddlewareBackupRestore restore2) {
                return restore2.getCreationTime().compareTo(restore1.getCreationTime());
            }
        };
        // 使用Collections类的sort方法对List<User>进行排序
        Collections.sort(restoreList, comparator);
    }

    // 排序并设置记录别名
    private List<MiddlewareBackupRecord> sortAndSetAliasName(List<MiddlewareBackupRecord> recordList, String orderBy) {
        recordList.sort((o1, o2) -> {
            if (StringUtils.isBlank(orderBy) || orderBy.split(",").length != 2) {
                return 0;
            }
            String[] order = orderBy.split(",");
            if ("size".equals(order[0])) {
                if (o1.getByteSize() == null) {
                    return 1;
                }
                if (o2.getByteSize() == null) {
                    return -1;
                }
                BigDecimal o1Size = new BigDecimal(o1.getByteSize());
                BigDecimal o2Size = new BigDecimal(o2.getByteSize());
                return o1Size.compareTo(o2Size) * (order[1].equals("desc") ? -1 : 1);
            } else if ("time".equals(order[0])) {
                return o1.getBackupTime() == null ? 1
                        : o2.getBackupTime() == null ? -1 : o2.getBackupTime().compareTo(o1.getBackupTime()) * ("desc".equals(order[1]) ? 1 : -1);
            } else {
                return 0;
            }
        });

        for (int i = 0; i < recordList.size(); i++) {
            MiddlewareBackupRecord bak = recordList.get(i);
            String[] bakNameSplit = bak.getBackupName().split("-");
            bak.setRecordName(bak.getTaskName() + "-" + bakNameSplit[bakNameSplit.length - 2] + "-" + bakNameSplit[bakNameSplit.length - 1]);
        }
        return recordList;
    }

    public static void main(String[] args) {
        List<Integer> s = new ArrayList<>();
        s.add(null);
        s.add(2);
        s.add(null);
        s.add(null);
        s.add(5);
        s.add(4);
        s.sort(((o1, o2) -> {
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            return o1.compareTo(o2);
        }));
        System.out.println("ok");
    }

    /**
     * 根据备份任务id查询周期备份任务名字列表
     *
     * @param clusterId
     * @param namespace
     * @param backupId
     * @return
     */
    private Set<String> listMiddlewareBackupScheduleNames(String clusterId, String namespace, String backupId) {
        return this.listMiddlewareBackupSchedule(clusterId, namespace, backupId).stream().map(middlewareBackupSchedule ->
                middlewareBackupSchedule.getMetadata().getName()).collect(Collectors.toSet());
    }

    /**
     * 根据备份任务id查询周期备份任务名字列表
     *
     * @param scheduleList
     * @return
     */
    private Set<String> listMiddlewareBackupScheduleNames(List<MiddlewareBackupSchedule> scheduleList) {
        return scheduleList.stream().map(middlewareBackupSchedule ->
                middlewareBackupSchedule.getMetadata().getName()).collect(Collectors.toSet());
    }

    /**
     * 返回一个map，周期备份名称:可用区别名
     *
     * @param clusterId
     * @param namespace
     * @param backupId
     * @return
     */
    private Map<String, MiddlewareBackupRecord> listMiddlewareBackupScheduleNamesMap(String clusterId, String namespace, String backupId) {
        List<MiddlewareBackupSchedule> schedules = listMiddlewareBackupSchedule(clusterId, namespace, backupId);
        Map<String, MiddlewareBackupRecord> scheduleMap = new HashMap<>();
        for (MiddlewareBackupSchedule schedule : schedules) {
            MiddlewareBackupRecord record = new MiddlewareBackupRecord();
            if (schedule.getMetadata().getLabels().containsKey("backupId")) {
                BeanMiddlewareBackupName backupName = backupNameService.getByBackupId(schedule.getMetadata().getLabels().get("backupId"));
                record.setTaskName(backupName.getBackupName());
            }
            if (schedule.getMetadata().getLabels().containsKey("activeArea")) {
                String activeArea = schedule.getMetadata().getLabels().get("activeArea");
                String activeAreaAliasName = getActiveAreaAliasName(clusterId, activeArea);
                record.setActiveArea(schedule.getMetadata().getLabels().get("activeArea"));
                record.setAreaAliasName(activeAreaAliasName);
                scheduleMap.put(schedule.getMetadata().getName(), record);
            }
            scheduleMap.put(schedule.getMetadata().getName(), record);
        }
        return scheduleMap;
    }

    /**
     * 根据备份任务id查询周期备份任务列表
     *
     * @param clusterId
     * @param namespace
     * @param backupId
     * @return
     */
    private List<MiddlewareBackupSchedule> listMiddlewareBackupSchedule(String clusterId, String namespace, String backupId) {
        Map<String, String> labels = new HashMap<>();
        labels.put("backupId", backupId);
        return backupScheduleCRDService.listByLabels(clusterId, namespace, labels);
    }

    /**
     * 根据备份任务id查询周期备份任务列表
     *
     * @param clusterId
     * @param namespace
     * @param backupId
     * @return
     */
    private List<MiddlewareBackup> listMiddlewareBackup(String clusterId, String namespace, String backupId) {
        Map<String, String> labels = new HashMap<>();
        labels.put("backupId", backupId);
        return backupCRDService.list(clusterId, namespace, labels);
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
     *
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
     *
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
            String positionIdStr = record.getPositionId();
            BeanBackupPosition backupPosition = null;
            if (MathUtil.isDigit(positionIdStr)) {
                backupPosition = backupPositionService.getBackupPosition(Integer.parseInt(positionIdStr));
            }
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
     * 转换克隆记录 List<MiddlewareRestoreCR> restoreCRList ->List<MiddlewareBackupRestore>
     *
     * @param restoreCRList
     * @param clusterId
     * @return
     */
    private List<MiddlewareBackupRestore> convertMiddlewareRestore(List<MiddlewareRestoreCR> restoreCRList, String clusterId) {
        List<MiddlewareBackupRestore> restoreList = restoreCRList.stream().map(restoreCR -> {
            MiddlewareBackupRestore backupRestore = new MiddlewareBackupRestore();
            backupRestore.setRestoreName(restoreCR.getMetadata().getName());
            backupRestore.setNamespace(restoreCR.getMetadata().getNamespace());
            MiddlewareRestoreStatus restoreCRStatus = restoreCR.getStatus();
            // 获取并设置克隆时间
            Date creationTime = DateUtils.parseUTCDate(restoreCR.getMetadata().getCreationTimestamp());
            backupRestore.setCreationTime(creationTime);
            if (restoreCRStatus != null) {
                backupRestore.setPhrase(restoreCR.getStatus().getPhase());
                backupRestore.setReason(restoreCR.getStatus().getReason());
                // 获取并设置克隆记录所在可用区
                Map<String, String> labels = restoreCR.getMetadata().getLabels();
                String activeArea = labels.get("activeArea");
                backupRestore.setActiveArea(activeAreaService.getAreaAliasName(clusterId, activeArea));
            }
            return backupRestore;
        }).collect(Collectors.toList());
        sortMiddlewareRestore(restoreList);
        return restoreList;
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
        Map<String, String> annotations = new HashMap<>();
        annotations.put("taskName", backupDTO.getTaskName());
        backupDTO.setAnnotations(annotations);
        backupDTO.setCrdType(middlewareCrdType);
    }

    /**
     * 查询备份记录(立即备份)
     */
    private List<MiddlewareBackup> getBackupRecordList(String clusterId, String namespace, String middlewareName,
                                                       String type) {
        List<MiddlewareBackup> resList = new ArrayList<>();
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
     *
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
    public boolean isIncBackup(MiddlewareBackupSchedule middlewareBackupSchedule) {
        AtomicBoolean isIncBackup = new AtomicBoolean(false);
        middlewareBackupSchedule.getSpec().getCustomBackups().forEach(cus -> {
            if (cus.containsKey(ENV)) {
                cus.get(ENV).forEach(env -> {
                    if (env.containsKey(VALUE) && env.get(VALUE).equals(BACKUP_INC)) {
                        isIncBackup.set(true);
                    }
                });
            }
        });
        return isIncBackup.get();
    }

    /**
     * 对象封装 List<MiddlewareBackupSchedule> -> List<MiddlewareBackupRecord>
     *
     * @param schedules
     * @return
     */
    public List<MiddlewareBackupRecord> convertBackupSchedulesToRecords(String clusterId, List<MiddlewareBackupSchedule> schedules) {
        List<MiddlewareBackupRecord> records = new ArrayList<>();
        schedules.forEach(backupSchedule -> {
            MiddlewareBackupRecord record = new MiddlewareBackupRecord();
            convertBackupScheduleToRecord(clusterId, backupSchedule, record);
            records.add(record);
        });
        return records;
    }

    public void convertBackupScheduleToRecord(MiddlewareBackupSchedule schedule, MiddlewareBackupRecord backupRecord) {
        convertBackupSchedule(null, schedule, backupRecord);
    }

    public void convertBackupScheduleToRecord(String clusterId, MiddlewareBackupSchedule schedule, MiddlewareBackupRecord backupRecord) {
        convertBackupSchedule(clusterId, schedule, backupRecord);
    }

    /**
     * 对象封装: MiddlewareBackupScheduleCR -> MiddlewareBackupRecord
     */
    public void convertBackupSchedule(String clusterId, MiddlewareBackupSchedule schedule, MiddlewareBackupRecord backupRecord) {
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
        if (StringUtils.isNotEmpty(schedule.getMetadata().getDeletionTimestamp())) {
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
        if (!CollectionUtils.isEmpty(labels)) {
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
        } catch (Exception e) {
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
        if (labels.containsKey("activeArea")) {
            String activeArea = labels.get("activeArea");
            backupRecord.setActiveArea(activeArea);
            backupRecord.setAreaAliasName(getActiveAreaAliasName(clusterId, activeArea));
            backupRecord.setActiveActive(true);
        } else {
            backupRecord.setActiveActive(false);
        }
    }

    /**
     * 对象封装 List<MiddlewareBackup> -> List<MiddlewareBackupRecord>
     *
     * @param clusterId
     * @param backups
     * @return
     */
    private List<MiddlewareBackupRecord> convertBackupsToRecords(String clusterId, List<MiddlewareBackup> backups) {
        List<MiddlewareBackupRecord> records = new ArrayList<>();
        backups.forEach(backup -> {
            MiddlewareBackupRecord record = new MiddlewareBackupRecord();
            convertBackupToRecord(clusterId, backup, record);
            records.add(record);
        });
        return records;
    }

    /**
     * 对象封装: MiddlewareBackupCR -> MiddlewareBackupRecord
     */
    public void convertBackupToRecord(String clusterId, MiddlewareBackup backup, MiddlewareBackupRecord backupRecord) {
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
        setMiddlewareBackupStatus(backup, backupRecord);

        backupRecord.setSourceType(middlewareCrTypeService.findTypeByCrType(backup.getSpec().getType()));
        // 设置备份存储大小
        setBackupSize(backupStatus, backupRecord);

        backupRecord.setAddressId(labels.get("addressId"));
        backupRecord.setSourceName(backup.getSpec().getName());
        backupRecord.setBackupMode("single");
        backupRecord.setSchedule(false);
        backupRecord.setOwner(labels.get(OWNER));
        // 设置备份任务可用区别名
        if (labels.containsKey("activeArea")) {
            String activeArea = labels.get("activeArea");
            backupRecord.setActiveArea(activeArea);
            backupRecord.setAreaAliasName(getActiveAreaAliasName(clusterId, activeArea));
            backupRecord.setActiveActive(true);
        } else {
            backupRecord.setActiveActive(false);
        }
    }

    /**
     * 设置备份记录状态
     * @param backup
     * @param backupRecord
     */
    private void setMiddlewareBackupStatus(MiddlewareBackup backup, MiddlewareBackupRecord backupRecord) {
        MiddlewareBackupStatus backupStatus = backup.getStatus();
        if("RecycleFailed".equals(backupStatus.getPhase())){
            backupRecord.setPhrase(backupStatus.getPhase());
            backupRecord.setReason(backupStatus.getReason());
            return;
        }
        if (StringUtils.isNotEmpty(backup.getMetadata().getDeletionTimestamp())) {
            backupRecord.setPhrase("Deleting");
            return;
        }
        if (!ObjectUtils.isEmpty(backupStatus)) {
            backupRecord.setPhrase(backupStatus.getPhase());
            if ("Failed".equals(backupStatus.getPhase())) {
                backupRecord.setReason(backupStatus.getReason());
            }
        } else {
            backupRecord.setPhrase("Unknown");
        }
    }

    /**
     * 返回备份存储大小
     * @param backupStatus
     * @param type
     * @return
     */
    private String getBackupSize(MiddlewareBackupStatus backupStatus, String type) {
        if (backupStatus != null && backupStatus.getStorageProvider() != null) {
            for (String key : backupStatus.getStorageProvider().keySet()) {
                JSONObject json = backupStatus.getStorageProvider().getJSONObject(key);
                if (json != null && json.containsKey("compressedSize")) {
                    return json.getString("compressedSize");
                }
            }
        }
        return null;
    }

    /**
     * 设置备份存储大小
     * @param backupStatus
     * @param backupRecord
     */
    private void setBackupSize(MiddlewareBackupStatus backupStatus, MiddlewareBackupRecord backupRecord) {
        String compressedSize = getBackupSize(backupStatus, backupRecord.getSourceType());
        if (compressedSize != null) {
            backupRecord.setSize(changeCompressedSizeUnit(compressedSize));
            backupRecord.setByteSize(compressedSize);
        }
    }

    /**
     * 设置备份存储大小
     * @param backupStatus
     * @param progressInfo
     * @param type
     */
    private void setBackupSize(MiddlewareBackupStatus backupStatus, ProgressInfo progressInfo, String type) {
        String compressedSize = getBackupSize(backupStatus, type);
        if (compressedSize != null) {
            progressInfo.setSize(changeCompressedSizeUnit(compressedSize));
            progressInfo.setByteSize(compressedSize);
        }
    }


    private String changeCompressedSizeUnit(String compressedSize) {
        List<MemoryUnitEnum> units = Arrays.asList(MemoryUnitEnum.TI, MemoryUnitEnum.GI, MemoryUnitEnum.MI, MemoryUnitEnum.KI);
        BigDecimal size = MemoryUnitEnum.toByte(compressedSize);
        for (MemoryUnitEnum u : units) {
            if (size.divide(u.toByte).compareTo(new BigDecimal("1")) >= 0) {
                return ResourceCalculationUtil.getResourceValue(compressedSize, DISK, u.unit) + u.name;
            }
        }
        return size.doubleValue() + MemoryUnitEnum.BYTE.name;
    }

    /**
     * 获取可用区别名
     *
     * @param clusterId
     * @param activeArea 可用区英文名
     * @return 可用区别名
     */
    private String getActiveAreaAliasName(String clusterId, String activeArea) {
        String activeAreaAliasName = activeAreaMap.get(activeArea);
        if (StringUtils.isEmpty(activeAreaAliasName)) {
            BeanActiveArea beanActiveArea = activeAreaService.get(clusterId, activeArea);
            if (beanActiveArea != null) {
                activeAreaMap.put(activeArea, beanActiveArea.getAliasName());
                return beanActiveArea.getAliasName();
            }
        }
        return activeAreaAliasName;
    }

    /**
     * 获取增量备份备份时间
     */
    public Date checkTimeExist(MiddlewareBackupSchedule schedule) {
        String type = middlewareCrTypeService.findTypeByCrType(schedule.getSpec().getType());
        if (schedule.getStatus() != null && schedule.getStatus().getStorageProvider() != null && schedule.getStatus().getStorageProvider().containsKey(type)) {
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
     *
     * @param backupDTO
     */
    public void checkBackupScheduleExist(MiddlewareBackupDTO backupDTO) {
        String backupId = backupDTO.getLabels().get("backupId");
        if (checkBackupScheduleExist(backupDTO.getClusterId(), backupDTO.getNamespace(), backupDTO.getMiddlewareName(), backupId)) {
            throw new BusinessException(ErrorMessage.MIDDLEWARE_BACKUP_SCHEDULE_EXIST);
        }
    }

    /**
     * 校验备份任务是否已存在
     *
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @param backupId
     * @return
     */
    public boolean checkBackupScheduleExist(String clusterId, String namespace, String middlewareName, String backupId) {
        MiddlewareBackupScheduleList list = backupScheduleCRDService.list(clusterId, namespace);
        if (list != null && !CollectionUtils.isEmpty(list.getItems())) {
            List<MiddlewareBackupSchedule> items = list.getItems();
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
     *
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
            recordGroup.setBackupAddresses(getBackupAddresses(records));
            BeanMiddlewareBackupName backupName = backupNameService.getByBackupId(record.getBackupId());
            if (backupName != null) {
                recordGroup.setTaskName(backupName.getBackupName());
            } else {
                recordGroup.setTaskName(record.getTaskName());
            }
            recordGroups.add(recordGroup);
        });
        return recordGroups;
    }

    /**
     * 获取备份位置地址数组
     *
     * @param records
     * @return
     */
    private List<String> getBackupAddresses(List<MiddlewareBackupRecord> records) {
        return records.stream().map(MiddlewareBackupRecord::getPosition).collect(Collectors.toList());
    }

    /**
     * 获取备份任务状态
     *
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
     *
     * @param records
     * @return
     */
    private Integer getTaskType(List<MiddlewareBackupRecord> records) {
        if (records.size() > 1) {
            return BackupTackTypeEnum.ACTIVE_ACTIVE.getType();
        } else {
            return BackupTackTypeEnum.NORMAL.getType();
        }
    }

    /**
     * 获取可用区选择器annotations
     *
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
     *
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
     *
     * @param records
     */
    private List<MiddlewareBackupRecord> filterByProject(List<MiddlewareBackupRecord> records, String organId, String projectId) {
        // 查询用户在当前项目下所有可见的中间件类型
        String username = CurrentUserRepository.getUser().getUsername();
        // 写入组织id
        if (StringUtils.isEmpty(organId)) {
            organId = RequestUtil.getOrganId();
        }
        // 根据分区过滤
        List<Namespace> namespaces = projectService.getNamespace(organId, projectId);
        if (!CollectionUtils.isEmpty(namespaces)) {
            Set<String> namespaceSet = namespaces.stream().map(Namespace::getName).collect(Collectors.toSet());
            records = records.stream().filter(record -> namespaceSet.contains(record.getNamespace())).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
        UserRole userRole = userService.getUserRole(username, organId, projectId);
        // 超级管理员在项目下没有角色，所以只有当用户为非超级管理员时才按中间件类型过滤
        if (userRole != null) {
            // 根据用户拥有运维权限当中间件类型类型过滤
            Set<String> middlewareSet = roleAuthorityService.listOpsMiddleware(userRole.getRoleId());
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
     *
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
