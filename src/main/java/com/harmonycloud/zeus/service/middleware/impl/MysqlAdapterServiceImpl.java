package com.harmonycloud.zeus.service.middleware.impl;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.model.MiddlewareBackupScheduleConfig;
import com.harmonycloud.caas.common.model.middleware.MiddlewareBackupRecord;
import com.harmonycloud.caas.common.model.middleware.MysqlBackupDto;
import com.harmonycloud.caas.common.model.middleware.ScheduleBackupConfig;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2021/10/26 4:16 下午
 */
@Service
@Slf4j
public class MysqlAdapterServiceImpl implements MiddlewareBackupService {

    @Autowired
    private MysqlServiceImpl mysqlService;

    @Override
    public List<MiddlewareBackupRecord> list(String clusterId, String namespace, String middlewareName, String type) {
        List<MysqlBackupDto> backups = mysqlService.listBackups(clusterId, namespace, middlewareName);
        List<MiddlewareBackupRecord> list = new ArrayList<>();
        for (MysqlBackupDto backup : backups) {
            MiddlewareBackupRecord record = new MiddlewareBackupRecord();
            record.setBackupName(backup.getBackupName());
            record.setBackupFileName(backup.getBackupFileName());
            record.setBackupTime(DateUtils.formatDate(backup.getDate(), DateType.YYYY_MM_DD_HH_MM_SS.getValue()));
            switch (backup.getStatus()){
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
            List<String> addressList = new ArrayList<>();
            addressList.add(backup.getPosition());
            record.setBackupAddressList(addressList);
            list.add(record);
        }
        return list;
    }

    @Override
    public BaseResult create(String clusterId, String namespace, String middlewareName, String type, String cron, Integer limitRecord) {
        if (StringUtils.isBlank(cron)) {
            mysqlService.createBackup(clusterId, namespace, middlewareName);
        } else {
            mysqlService.createScheduleBackup(clusterId, namespace, middlewareName, limitRecord, cron);
        }
        return BaseResult.ok();
    }

    @Override
    public BaseResult update(String clusterId, String namespace, String middlewareName, String type, String cron, Integer limitRecord, String pause) {
        mysqlService.createScheduleBackup(clusterId, namespace, middlewareName, limitRecord, cron);
        return BaseResult.ok();
    }

    @Override
    public BaseResult delete(String clusterId, String namespace,String middlewareName, String type, String backupName, String backupFileName) {
        try {
            mysqlService.deleteBackup(clusterId, namespace, middlewareName, backupFileName, backupName);
        } catch (Exception e) {
            log.error("删除备份失败", e);
        }
        return BaseResult.ok();
    }

    @Override
    public BaseResult get(String clusterId, String namespace, String middlewareName, String type) {
        ScheduleBackupConfig scheduleBackup = mysqlService.getScheduleBackups(clusterId, namespace, middlewareName);
        MiddlewareBackupScheduleConfig config = new MiddlewareBackupScheduleConfig();
        config.setCanPause(false);
        if (scheduleBackup == null) {
            config.setConfiged(false);
        } else {
            config.setConfiged(true);
            config.setCron(scheduleBackup.getCron());
            config.setLimitRecord(scheduleBackup.getKeepBackups());
            config.setNextBackupTime(DateUtils.formatDate(scheduleBackup.getNextBackupDate(), DateType.YYYY_MM_DD_HH_MM_SS.getValue()));
        }
        return BaseResult.ok(config);
    }

    @Override
    public BaseResult createScheduleBackup(String clusterId, String namespace, String middlewareName, String crdType, String middlewareRealName, String cron, Integer limitRecord, Map<String, String> labels) {
        return null;
    }

    @Override
    public BaseResult createNormalBackup(String clusterId, String namespace, String middlewareName, String crdType, String middlewareRealName, Map<String, String> labels) {
        return null;
    }

    @Override
    public BaseResult createRestore(String clusterId, String namespace, String middlewareName, String type, String restoreName, String backupName, String backupFileName, String aliasName) {
        return null;
    }

    @Override
    public void tryCreateMiddlewareRestore(String clusterId, String namespace, String type, String middlewareName, String backupName, String restoreName) {

    }

    @Override
    public void deleteMiddlewareBackupInfo(String clusterId, String namespace, String type, String middlewareName) {

    }
}
