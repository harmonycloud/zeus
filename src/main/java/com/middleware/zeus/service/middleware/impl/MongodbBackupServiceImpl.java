package com.middleware.zeus.service.middleware.impl;

import com.middleware.zeus.common.model.Secret;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbBackupRecordDo;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbScheduleBackupDto;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbBackupServerDto;
import com.middleware.zeus.integration.cluster.MongodbWrapper;
import com.middleware.zeus.integration.cluster.OpsManagerWrapper;
import com.middleware.zeus.integration.cluster.bean.mongodb.Mongodb;
import com.middleware.zeus.integration.cluster.bean.mongodb.MongodbSpec;
import com.middleware.zeus.integration.cluster.bean.mongodb.OpsManager;
import com.middleware.zeus.integration.cluster.bean.mongodb.OpsManagerSpec;
import com.middleware.zeus.integration.dashboard.MongodbClientWrapper;
import com.middleware.zeus.service.k8s.SecretService;
import com.middleware.zeus.service.middleware.MongodbBackupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.middleware.zeus.common.constants.NameConstant.ACCESS_KEY;
import static com.middleware.zeus.common.constants.NameConstant.SECRET_KEY;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_OPERATOR;

/**
 * @author xutianhong
 * @Date 2025/7/31 10:06
 */
@Slf4j
@Service
public class MongodbBackupServiceImpl implements MongodbBackupService {

    @Autowired
    private MongodbClientWrapper mongodbClientWrapper;

    @Autowired
    private OpsManagerWrapper opsManagerWrapper;

    @Autowired
    private MongodbWrapper mongodbWrapper;

    @Autowired
    private SecretService secretService;

    @Override
    public void updateBackupServer(MongodbBackupServerDto mongodbBackupServerDto) {
        String clusterId = mongodbBackupServerDto.getClusterId();
        OpsManager opsManager = opsManagerWrapper.get(clusterId, MIDDLEWARE_OPERATOR, "mongodb-enterprise-operator-om");
        if (opsManager == null) {
            log.error("OpsManager is null, clusterId:{}", clusterId);
            return;
        }
        // 数据结构转换
        OpsManagerSpec.Backup.Store s3OpLogStore = new OpsManagerSpec.Backup.Store(mongodbBackupServerDto.getS3OpLogBackup());
        OpsManagerSpec.Backup.Store s3Store = new OpsManagerSpec.Backup.Store(mongodbBackupServerDto.getS3Backup());

        // 为s3OpLog备份使用的minio生成鉴权secret
        secretService.genericSecretWithUsername(clusterId, MIDDLEWARE_OPERATOR, "s3OpLogStore-minio", mongodbBackupServerDto.getS3OpLogBackup().getUsername(), mongodbBackupServerDto.getS3OpLogBackup().getPassword());
        // 为s3数据备份使用的minio生成鉴权secret
        secretService.genericSecretWithUsername(clusterId, MIDDLEWARE_OPERATOR, "s3Store-minio", mongodbBackupServerDto.getS3Backup().getUsername(), mongodbBackupServerDto.getS3Backup().getPassword());

        s3OpLogStore.getS3SecretRef().setName("s3OpLogStore-minio");
        s3Store.getS3SecretRef().setName("s3Store-minio");

        // 为s3OpLog备份使用的minio生成tls secret
        secretService.genericSecretWithConf(clusterId, MIDDLEWARE_OPERATOR, "s3OpLogStore-cert", mongodbBackupServerDto.getS3OpLogBackup().getPemName(), mongodbBackupServerDto.getS3OpLogBackup().getPemContent());
        // 为s3数据备份使用的minio生成tls secret
        secretService.genericSecretWithConf(clusterId, MIDDLEWARE_OPERATOR, "s3Store-cert", mongodbBackupServerDto.getS3Backup().getPemName(), mongodbBackupServerDto.getS3Backup().getPemContent());

        OpsManagerSpec.Backup.Store.CustomCertificateSecretRef s3OpLogCert = new OpsManagerSpec.Backup.Store.CustomCertificateSecretRef();
        s3OpLogCert.setKey(mongodbBackupServerDto.getS3OpLogBackup().getPemName());
        s3OpLogCert.setName("s3OpLogStore-cert");
        s3OpLogStore.setCustomCertificateSecretRefs(List.of(s3OpLogCert));

        OpsManagerSpec.Backup.Store.CustomCertificateSecretRef s3Cert = new OpsManagerSpec.Backup.Store.CustomCertificateSecretRef();
        s3Cert.setKey(mongodbBackupServerDto.getS3Backup().getPemName());
        s3Cert.setName("s3Store-cert");
        s3Store.setCustomCertificateSecretRefs(List.of(s3Cert));

        OpsManagerSpec.Backup backup = opsManager.getSpec().getBackup();
        if (backup == null){
            backup = new OpsManagerSpec.Backup();
        }
        backup.setEnabled(true);
        backup.setS3Stores(List.of(s3Store));
        backup.setS3OpLogStores(List.of(s3OpLogStore));

        opsManagerWrapper.patch(clusterId, opsManager);
    }

    @Override
    public MongodbBackupServerDto getBackupServer(String clusterId) {
        OpsManager opsManager = opsManagerWrapper.get(clusterId, MIDDLEWARE_OPERATOR, "mongodb-enterprise-operator-om");
        if (opsManager == null) {
            log.error("OpsManager is null, clusterId:{}", clusterId);
            return null;
        }
        MongodbBackupServerDto mongodbBackupServerDto = new MongodbBackupServerDto();

        MongodbBackupServerDto.BackupSerer s3OpLogBackup = new MongodbBackupServerDto.BackupSerer(opsManager.getSpec().getBackup().getS3OpLogStores().get(0));
        MongodbBackupServerDto.BackupSerer s3Backup = new MongodbBackupServerDto.BackupSerer(opsManager.getSpec().getBackup().getS3Stores().get(0));

        // 获取minio的账号密码
        Secret s3OpLogStore = secretService.get(clusterId, MIDDLEWARE_OPERATOR, "s3OpLogStore-minio");
        Map<String, String> s3OpLogData = s3OpLogStore.getData();
        s3OpLogBackup.setUsername(s3OpLogData.get(ACCESS_KEY));
        s3OpLogBackup.setPassword(s3OpLogData.get(SECRET_KEY));

        Secret s3Store = secretService.get(clusterId, MIDDLEWARE_OPERATOR, "s3Store-minio");
        Map<String, String> s3Data = s3Store.getData();
        s3Backup.setUsername(s3Data.get(ACCESS_KEY));
        s3Backup.setPassword(s3Data.get(SECRET_KEY));

        // 获取证书文件名称
        Secret s3OpLogStoreCert = secretService.get(clusterId, MIDDLEWARE_OPERATOR, "s3OpLogStore-cert");
        Map<String, String> s3OpLogStoreCertData = s3OpLogStoreCert.getData();
        s3OpLogBackup.setPemName(s3OpLogStoreCertData.keySet().iterator().next());

        Secret s3StoreCert = secretService.get(clusterId, MIDDLEWARE_OPERATOR, "s3Store-cert");
        Map<String, String> s3StoreCertData = s3StoreCert.getData();
        s3Backup.setPemName(s3StoreCertData.keySet().iterator().next());

        mongodbBackupServerDto.setClusterId(clusterId);
        mongodbBackupServerDto.setS3OpLogBackup(s3OpLogBackup);
        mongodbBackupServerDto.setS3Backup(s3Backup);
        return mongodbBackupServerDto;
    }

    @Override
    public void enableScheduleBackup(MongodbScheduleBackupDto mongodbScheduleBackupDto) {
        String clusterId = mongodbScheduleBackupDto.getClusterId();
        String namespace = mongodbScheduleBackupDto.getNamespace();
        String name = mongodbScheduleBackupDto.getName();
        Mongodb mongodb = mongodbWrapper.get(clusterId, namespace, name);
        if (mongodb == null) {
            log.error("Mongodb is null, clusterId:{}, namespace:{}, name:{}", clusterId, namespace, name);
            return;
        }

        MongodbSpec.Backup.SnapshotSchedule snapshotSchedule = new MongodbSpec.Backup.SnapshotSchedule();
        BeanUtils.copyProperties(mongodbScheduleBackupDto, snapshotSchedule);

        MongodbSpec.Backup backup = new MongodbSpec.Backup();
        backup.setMode("enabled");
        backup.setSnapshotSchedule(snapshotSchedule);
        mongodb.getSpec().setBackup(backup);

        mongodbWrapper.patch(clusterId, mongodb);
    }

    @Override
    public void disableScheduleBackup(String clusterId, String namespace, String name) {
        Mongodb mongodb = mongodbWrapper.get(clusterId, namespace, name);
        mongodb.getSpec().getBackup().setMode("disabled");

        mongodbWrapper.patch(clusterId, mongodb);
    }

    @Override
    public MongodbScheduleBackupDto getScheduleBackup(String clusterId, String namespace, String name) {
        Mongodb mongodb = mongodbWrapper.get(clusterId, namespace, name);
        if (mongodb == null) {
            return null;
        }
        MongodbSpec.Backup backup = mongodb.getSpec().getBackup();
        if (backup == null) {
            return null;
        }
        MongodbSpec.Backup.SnapshotSchedule snapshotSchedule = backup.getSnapshotSchedule();
        if (snapshotSchedule == null) {
            return null;
        }
        MongodbScheduleBackupDto mongodbScheduleBackupDto = new MongodbScheduleBackupDto();
        BeanUtils.copyProperties(snapshotSchedule, mongodbScheduleBackupDto);
        mongodbScheduleBackupDto.setClusterId(clusterId);
        mongodbScheduleBackupDto.setNamespace(namespace);
        mongodbScheduleBackupDto.setName(name);
        return mongodbScheduleBackupDto;
    }

    @Override
    public void singleBackup(String clusterId, String namespace, String name, Integer retentionDays) {

    }

    @Override
    public MongodbBackupRecordDo backupList(String clusterId, String namespace, String name) {
        return null;
    }

    @Override
    public void createRestore(String clusterId, String namespace, String name, String snapshotId) {

    }
}
