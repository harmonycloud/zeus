package com.harmonycloud.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.MiddlewareBackupScheduleConfig;
import com.harmonycloud.caas.common.model.middleware.MiddlewareBackup;
import com.harmonycloud.caas.common.model.middleware.ScheduleBackupConfig;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupCRD;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupSpec;
import com.harmonycloud.zeus.service.k8s.MiddlewareBackupCRDService;
import com.harmonycloud.zeus.service.middleware.AbstractMiddlewareService;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupService;
import com.harmonycloud.zeus.util.CronUtils;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/24
 */
@Slf4j
@Service
public class MiddlewareBackupServiceImpl implements MiddlewareBackupService {

    @Autowired
    private MiddlewareBackupCRDService middlewareBackupCRDService;

    @Override
    public List list(String clusterId, String namespace, String type, String middlewareName) {

        return null;
    }

    @Override
    public BaseResult create(String clusterId, String namespace, String type, String middlewareName, String cron, Integer limitRecord) {
        String backupName = MiddlewareTypeEnum.findByType(type).getMiddlewareCrdType() + "-" + middlewareName;

        MiddlewareBackupCRD middlewareBackupCRD = new MiddlewareBackupCRD();
        middlewareBackupCRD.setKind("MiddlewareBackup");
        ObjectMeta metaData = new ObjectMeta();
        metaData.setNamespace(namespace);
        metaData.setName(backupName + "-backup");
        middlewareBackupCRD.setMetadata(metaData);

        MiddlewareBackupSpec middlewareBackupSpec = new MiddlewareBackupSpec(backupName, cron, limitRecord);
        middlewareBackupSpec.setName(backupName);
        middlewareBackupCRD.setSpec(middlewareBackupSpec);

        try {
            middlewareBackupCRDService.create(clusterId, middlewareBackupCRD);
            return BaseResult.ok();
        } catch (IOException e) {
            log.error("备份创建失败", e);
            return BaseResult.error();
        }
    }

    @Override
    public BaseResult update(String clusterId, String namespace, String type, String middlewareName, String cron, Integer limitRecord) {
        String backupName = MiddlewareTypeEnum.findByType(type).getMiddlewareCrdType() + "-" + middlewareName + "-backup";
        MiddlewareBackupCRD middlewareBackupCRD = middlewareBackupCRDService.get(clusterId, namespace, backupName);
        try {
            middlewareBackupCRDService.update(clusterId, middlewareBackupCRD);
            return BaseResult.ok();
        } catch (IOException e) {
            log.error("中间件{}备份设置更新失败", middlewareName);
        }
        return BaseResult.error();
    }

    @Override
    public BaseResult get(String clusterId, String namespace, String type, String middlewareName) {
        String backupName = MiddlewareTypeEnum.findByType(type).getMiddlewareCrdType() + "-" + middlewareName + "-backup";
        MiddlewareBackupCRD middlewareBackupCRD = middlewareBackupCRDService.get(clusterId, namespace, backupName);
        MiddlewareBackupSpec spec = middlewareBackupCRD.getSpec();
        MiddlewareBackupSpec.Schedule schedule = spec.getSchedule();
        if (schedule != null) {
            MiddlewareBackupScheduleConfig config = new MiddlewareBackupScheduleConfig(schedule.getCron(), schedule.getLimitRecord(), CronUtils.calculateNextDate(schedule.getCron()));
            return BaseResult.ok(config);
        }
        return BaseResult.ok();
    }
}
