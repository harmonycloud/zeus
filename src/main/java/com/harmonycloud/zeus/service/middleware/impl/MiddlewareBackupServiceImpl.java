package com.harmonycloud.zeus.service.middleware.impl;

import cn.hutool.db.ds.pooled.PooledDSFactory;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.MiddlewareBackupScheduleConfig;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.operator.impl.BaseOperatorImpl;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.util.CronUtils;
import com.harmonycloud.zeus.util.DateUtil;
import com.harmonycloud.zeus.util.SortUtils;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * 中间件备份
 *
 * @author liyinlong
 * @since 2021/9/15 3:22 下午
 */
@Slf4j
@Service
public class MiddlewareBackupServiceImpl implements MiddlewareBackupService {

    @Autowired
    private MiddlewareBackupScheduleCRDService backupScheduleCRDService;
    @Autowired
    private MiddlewareBackupCRDService backupCRDService;
    @Autowired
    private MiddlewareRestoreCRDService restoreCRDService;
    @Autowired
    private BaseOperatorImpl baseOperator;
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private MiddlewareCRDService middlewareCRDService;
    @Autowired
    private ServiceService serviceService;

    @Override
    public List<MiddlewareBackupRecord> list(String clusterId, String namespace, String middlewareName, String type) {
        String middlewareRealName = getRealMiddlewareName(type, middlewareName);
        Map<String, String> labels = new HashMap<>();
        labels.put("owner", middlewareRealName + "-backup");
        List<MiddlewareBackupRecord> recordList = new ArrayList<>();
        MiddlewareBackupList middlewareBackupList = backupCRDService.list(clusterId, namespace, labels);
        if (middlewareBackupList != null && !CollectionUtils.isEmpty(middlewareBackupList.getItems())) {
            List<MiddlewareBackupCRD> items = middlewareBackupList.getItems();
            items.forEach(item -> {
                String backupName = item.getMetadata().getName();
                MiddlewareBackupStatus backupStatus = item.getStatus();
                MiddlewareBackupRecord backupRecord = new MiddlewareBackupRecord();
                String backupTime = DateUtil.utc2Local(item.getMetadata().getCreationTimestamp(), DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
                backupRecord.setBackupTime(backupTime);
                backupRecord.setBackupName(backupName);
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
    public BaseResult create(String clusterId, String namespace, String middlewareName, String type, String cron, Integer limitRecord) {
        String middlewareRealName = getRealMiddlewareName(type, middlewareName);
        String middlewareCrdType = MiddlewareTypeEnum.findByType(type).getMiddlewareCrdType();
        Map<String, String> backupLabel = getBackupLabel(middlewareName, type);
        Map<String, String> middlewareLabel = getBackupLabel(middlewareName, type);
        backupLabel.putAll(middlewareLabel);
        if (StringUtils.isBlank(cron)) {
            return createNormalBackup(clusterId, namespace, middlewareName, middlewareCrdType, middlewareRealName, backupLabel);
        } else {
            return createScheduleBackup(clusterId, namespace, middlewareName, middlewareCrdType, middlewareRealName, cron, limitRecord, backupLabel);
        }
    }

    @Override
    public BaseResult update(String clusterId, String namespace, String middlewareName, String type, String cron, Integer limitRecord, String pause) {
        String backupName = getRealMiddlewareName(type, middlewareName) + "-backup";
        MiddlewareBackupScheduleCRD middlewareBackupScheduleCRD = backupScheduleCRDService.get(clusterId, namespace, backupName);
        try {
            MiddlewareBackupScheduleSpec spec = middlewareBackupScheduleCRD.getSpec();
            spec.getSchedule().setCron(CronUtils.parseUtcCron(cron));
            spec.getSchedule().setLimitRecord(limitRecord);
            spec.setPause(pause);
            backupScheduleCRDService.update(clusterId, middlewareBackupScheduleCRD);
            return BaseResult.ok();
        } catch (IOException e) {
            log.error("中间件{}备份设置更新失败", middlewareName);
            return BaseResult.error();
        }
    }

    @Override
    public BaseResult delete(String clusterId, String namespace, String backupName) {
        try {
            backupCRDService.delete(clusterId, namespace, backupName);
            return BaseResult.ok();
        } catch (IOException e) {
            log.error("删除备份记录失败");
            return BaseResult.error();
        }
    }

    @Override
    public BaseResult get(String clusterId, String namespace, String middlewareName, String type) {
        String backupName = MiddlewareTypeEnum.findByType(type).getMiddlewareCrdType() + "-" + middlewareName + "-backup";
        MiddlewareBackupScheduleCRD middlewareBackupScheduleCRD = backupScheduleCRDService.get(clusterId, namespace, backupName);
        MiddlewareBackupScheduleConfig config;
        if (middlewareBackupScheduleCRD == null) {
            config = new MiddlewareBackupScheduleConfig();
            config.setConfiged(false);
            return BaseResult.ok(config);
        }
        MiddlewareBackupScheduleSpec spec = middlewareBackupScheduleCRD.getSpec();
        if (spec != null) {
            String localCron = CronUtils.parseLocalCron(spec.getSchedule().getCron());
            config = new MiddlewareBackupScheduleConfig(localCron,
                    spec.getSchedule().getLimitRecord(), CronUtils.calculateNextDate(localCron), spec.getPause(), true);
            return BaseResult.ok(config);
        }
        return BaseResult.ok();
    }


    public BaseResult createScheduleBackup(String clusterId, String namespace, String middlewareName, String crdType, String middlewareRealName, String cron, Integer limitRecord, Map<String, String> labels) {
        MiddlewareBackupScheduleCRD middlewareBackupScheduleCRD = new MiddlewareBackupScheduleCRD();
        middlewareBackupScheduleCRD.setKind("MiddlewareBackupSchedule");
        String backupName = middlewareRealName + "-backup";
        ObjectMeta meta = getMiddlewareBackupLabels(namespace, backupName, middlewareRealName, labels);
        middlewareBackupScheduleCRD.setMetadata(meta);
        MiddlewareBackupScheduleSpec middlewareBackupScheduleSpec = new MiddlewareBackupScheduleSpec(middlewareName, crdType, CronUtils.parseUtcCron(cron), limitRecord);
        middlewareBackupScheduleSpec.setName(middlewareName);
        middlewareBackupScheduleSpec.setType(crdType);
        middlewareBackupScheduleSpec.setPause("off");
        middlewareBackupScheduleCRD.setSpec(middlewareBackupScheduleSpec);
        try {
            backupScheduleCRDService.create(clusterId, middlewareBackupScheduleCRD);
            return BaseResult.ok();
        } catch (IOException e) {
            log.error("备份创建失败", e);
            return BaseResult.error();
        }
    }

    public BaseResult createNormalBackup(String clusterId, String namespace, String middlewareName, String crdType, String middlewareRealName, Map<String, String> labels) {
        MiddlewareBackupCRD middlewareBackupCRD = new MiddlewareBackupCRD();
        middlewareBackupCRD.setKind("MiddlewareBackup");
        String backupName = middlewareRealName + "-" + UUIDUtils.get8UUID();
        ObjectMeta meta = getMiddlewareBackupLabels(namespace, backupName, middlewareRealName, labels);
        middlewareBackupCRD.setMetadata(meta);
        MiddlewareBackupSpec spec = new MiddlewareBackupSpec();
        spec.setName(middlewareName);
        spec.setType(crdType);
        middlewareBackupCRD.setSpec(spec);
        try {
            backupCRDService.create(clusterId, middlewareBackupCRD);
            return BaseResult.ok();
        } catch (IOException e) {
            log.error("立即备份失败", e);
            return BaseResult.error();
        }
    }

    /**
     * 获取中间件备份labels
     *
     * @param backupName         备份名称
     * @param namespace          命名空间
     * @param middlewareRealName 中间件名称
     * @return
     */
    public ObjectMeta getMiddlewareBackupLabels(String namespace, String backupName, String middlewareRealName, Map<String, String> labels) {
        ObjectMeta metaData = new ObjectMeta();
        metaData.setNamespace(namespace);
        metaData.setName(backupName);
        Map<String, String> backupLabel = getBackupLabel(middlewareRealName);
        backupLabel.putAll(labels);
        metaData.setLabels(backupLabel);
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
    public BaseResult createRestore(String clusterId, String namespace, String middlewareName, String type, String restoreName, String backupName, String aliasName) {
        //检查服务是否已存在
        Middleware middleware = middlewareService.detail(clusterId, namespace, middlewareName, type);
        fixStorageUnit(middleware);
        middleware.setName(restoreName);
        middleware.setClusterId(clusterId);
        MiddlewareClusterDTO cluster = clusterService.findByIdAndCheckRegistry(middleware.getClusterId());
        baseOperator.createPreCheck(middleware, cluster);
        //设置中间件恢复信息
        try {
            middleware.setName(restoreName);
            middleware.setAliasName(aliasName);
            middleware.setChartName(type);
            middlewareService.create(middleware);
            tryCreateMiddlewareRestore(clusterId, namespace, type, middlewareName, backupName, restoreName);
            return BaseResult.ok();
        } catch (Exception e) {
            log.error("备份服务创建失败", e);
            return BaseResult.error();
        }
    }

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
                createMiddlewareRestore(clusterId, namespace, type, middlewareName, backupName, restoreName, status);
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

    /**
     * 创建中间件恢复
     *
     * @param clusterId      集群id
     * @param namespace      分区
     * @param type           中间件类型
     * @param middlewareName 源中间件名称
     * @param backupName     备份名称
     * @param restoreName    恢复中间件名称
     * @param status         恢复中间件状态信息
     */
    public void createMiddlewareRestore(String clusterId, String namespace, String type, String middlewareName, String backupName, String restoreName, MiddlewareStatus status) {
        try {
            List<MiddlewareInfo> podList = status.getInclude().get(MiddlewareConstant.PODS);
            List<MiddlewareInfo> pvcs = status.getInclude().get(MiddlewareConstant.PERSISTENT_VOLUME_CLAIMS);
            List<String> pods = new ArrayList<>();
            podList.forEach(pod -> {
                pods.add(pod.getName());
            });
            MiddlewareBackupCRD backup = backupCRDService.get(clusterId, namespace, backupName);
            List<MiddlewareBackupStatus.BackupInfo> backupInfos = backup.getStatus().getBackupInfos();
            List<String> pvcList = orderPvc(pvcs);
            List<String> backupInfoList = orderBackupInfo(backupInfos);
            MiddlewareRestoreCRD crd = new MiddlewareRestoreCRD();
            ObjectMeta meta = new ObjectMeta();
            meta.setNamespace(namespace);
            meta.setName(getRestoreName(type, middlewareName));
            Map<String, String> backupLabel = getBackupLabel(restoreName, type);
            Map<String, String> middlewareLabel = getBackupLabel(middlewareName, type);
            backupLabel.putAll(middlewareLabel);
            meta.setLabels(backupLabel);
            crd.setMetadata(meta);
            MiddlewareRestoreSpec spec = new MiddlewareRestoreSpec();
            spec.setBackupName(backupName);
            spec.setName(getRealMiddlewareName(type, restoreName));
            spec.setType(MiddlewareTypeEnum.findByType(type).getMiddlewareCrdType());
            spec.setPods(pods);
            spec.setRestoreBind(convertRestoreBind(backupInfoList, pvcList));
            crd.setSpec(spec);
            restoreCRDService.create(clusterId, crd);
        } catch (IOException e) {
            log.error("创建恢复实例出错了", e);
        }
    }

    /**
     * 转换并返回RestoreBind信息
     *
     * @param backupInfos 中间件备份backupInfos
     * @param pvcs        中间件pvc
     * @return
     */
    private Map<String, String> convertRestoreBind(List<String> backupInfos, List<String> pvcs) {
        Map<String, String> restoreBind = new HashMap<>();
        for (int i = 0; i < pvcs.size(); i++) {
            restoreBind.put(pvcs.get(i), backupInfos.get(i));
        }
        return restoreBind;
    }

    /**
     * 对BackupInfo信息进行排序
     *
     * @param backupInfos
     */
    private List<String> orderBackupInfo(List<MiddlewareBackupStatus.BackupInfo> backupInfos) {
        List<String> list = new ArrayList<>();
        if (backupInfos.size() == 0) {
            return list;
        }
        String[] temps = new String[backupInfos.size()];
        for (int i = 0; i < backupInfos.size(); i++) {
            temps[i] = backupInfos.get(i).getVolumeSnapshot();
        }
        SortUtils.quickSort(temps, 0, backupInfos.size() - 1);
        for (int i = 0; i < temps.length; i++) {
            list.add(temps[i]);
        }
        return list;
    }

    /**
     * 对pvc信息进行排序
     *
     * @param pvcs
     */
    private List<String> orderPvc(List<MiddlewareInfo> pvcs) {
        List<String> list = new ArrayList<>();
        if (pvcs.size() == 0) {
            return list;
        }
        String[] temps = new String[pvcs.size()];
        for (int i = 0; i < pvcs.size(); i++) {
            temps[i] = pvcs.get(i).getName();
        }
        SortUtils.quickSort(temps, 0, pvcs.size() - 1);
        for (int i = 0; i < temps.length; i++) {
            list.add(temps[i]);
        }
        return list;
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
                if (v.getStorageClassQuota() != null)
                    v.setStorageClassQuota(v.getStorageClassQuota().replaceAll("Gi", ""));
            });
        }
    }

}
