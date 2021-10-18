package com.harmonycloud.zeus.service.middleware.impl;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.MiddlewareBackupScheduleConfig;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareBackupRecord;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.integration.cluster.bean.*;
import com.harmonycloud.zeus.operator.impl.BaseOperatorImpl;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.MiddlewareBackupCRDService;
import com.harmonycloud.zeus.service.k8s.MiddlewareBackupScheduleCRDService;
import com.harmonycloud.zeus.service.k8s.MiddlewareRestoreCRDService;
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
                    String backupAddressPrefix = minio.getUrl() + "/" + minio.getBucket() + "/" + minio.getPrefix();
                    List<MiddlewareBackupStatus.BackupInfo> backupInfos = backupStatus.getBackupInfos();
                    if (backupInfos != null) {
                        List<String> backupAddressList = new ArrayList<>();
                        for (MiddlewareBackupStatus.BackupInfo backupInfo : backupInfos) {
                            if (!StringUtils.isBlank(backupInfo.getRepository())) {
                                backupAddressList.add(backupAddressPrefix + "-" + backupInfo.getRepository());
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
        if (StringUtils.isBlank(cron)) {
            return createNormalBackup(clusterId, namespace, middlewareName, middlewareCrdType, middlewareRealName);
        } else {
            return createScheduleBackup(clusterId, namespace, middlewareName, middlewareCrdType, middlewareRealName, cron, limitRecord);
        }
    }

    @Override
    public BaseResult update(String clusterId, String namespace, String middlewareName, String type, String cron, Integer limitRecord, String pause) {
        String backupName = getRealMiddlewareName(type, middlewareName) + "-backup";
        MiddlewareBackupScheduleCRD middlewareBackupScheduleCRD = backupScheduleCRDService.get(clusterId, namespace, backupName);
        try {
            MiddlewareBackupScheduleSpec spec = middlewareBackupScheduleCRD.getSpec();
            spec.getSchedule().setCron(cron);
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
            config = new MiddlewareBackupScheduleConfig(spec.getSchedule().getCron(),
                    spec.getSchedule().getLimitRecord(), CronUtils.calculateNextDate(spec.getSchedule().getCron()), spec.getPause(), true);
            return BaseResult.ok(config);
        }
        return BaseResult.ok();
    }


    public BaseResult createScheduleBackup(String clusterId, String namespace, String middlewareName, String crdType, String middlewareRealName, String cron, Integer limitRecord) {
        MiddlewareBackupScheduleCRD middlewareBackupScheduleCRD = new MiddlewareBackupScheduleCRD();
        middlewareBackupScheduleCRD.setKind("MiddlewareBackupSchedule");
        String backupName = middlewareRealName + "-backup";
        middlewareBackupScheduleCRD.setMetadata(getMiddlewareLabels(namespace, backupName, middlewareRealName));

        MiddlewareBackupScheduleSpec middlewareBackupScheduleSpec = new MiddlewareBackupScheduleSpec(middlewareName, crdType, cron, limitRecord);
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

    public BaseResult createNormalBackup(String clusterId, String namespace, String middlewareName, String crdType, String middlewareRealName) {
        MiddlewareBackupCRD middlewareBackupCRD = new MiddlewareBackupCRD();
        middlewareBackupCRD.setKind("MiddlewareBackup");
        String backupName = middlewareRealName + "-" + UUIDUtils.get8UUID();
        middlewareBackupCRD.setMetadata(getMiddlewareLabels(namespace, backupName, middlewareRealName));
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
     * 获取中间件labels
     *
     * @param backupName         备份名称
     * @param namespace          命名空间
     * @param middlewareRealName 中间件名称
     * @return
     */
    public ObjectMeta getMiddlewareLabels(String namespace, String backupName, String middlewareRealName) {
        ObjectMeta metaData = new ObjectMeta();
        metaData.setNamespace(namespace);
        metaData.setName(backupName);
        Map<String, String> labels = new HashMap<>();
        labels.put("owner", middlewareRealName + "-backup");
        metaData.setLabels(labels);
        return metaData;
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
        middleware.setName(restoreName);
        middleware.setClusterId(clusterId);
        MiddlewareClusterDTO cluster = clusterService.findByIdAndCheckRegistry(middleware.getClusterId());
        baseOperator.createPreCheck(middleware, cluster);
        //设置中间件恢复信息
        MiddlewareRestoreCRD crd = new MiddlewareRestoreCRD();
        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace(namespace);
        meta.setName(getRestoreName(type, middlewareName));
        crd.setMetadata(meta);
        //创建中间件恢复
        MiddlewareRestoreSpec spec = new MiddlewareRestoreSpec();
        spec.setBackupName(backupName);
        spec.setName(getRealMiddlewareName(type, restoreName));
        spec.setType(MiddlewareTypeEnum.findByType(type).getMiddlewareCrdType());
        crd.setSpec(spec);
        try {
            restoreCRDService.create(clusterId, crd);
            middleware.setName(restoreName);
            middleware.setAliasName(aliasName);
            middleware.setChartName(type);
            middlewareService.create(middleware);
            return BaseResult.ok();
        } catch (IOException e) {
            log.error("备份服务创建失败");
            return BaseResult.error();
        }
    }
}
