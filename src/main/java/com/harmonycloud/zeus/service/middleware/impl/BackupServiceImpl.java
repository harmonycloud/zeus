package com.harmonycloud.zeus.service.middleware.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.harmonycloud.zeus.integration.cluster.bean.BackupStorageProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.caas.common.model.middleware.Backup;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.integration.cluster.BackupWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.BackupCRD;
import com.harmonycloud.zeus.service.middleware.BackupService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2021/4/2 4:23 下午
 */
@Service
@Slf4j
public class BackupServiceImpl implements BackupService {

    @Autowired
    private BackupWrapper backupWrapper;

    /**
     * 查询备份列表
     *
     * @param clusterId
     * @param namespace
     * @return List<BackupDto>
     */
    @Override
    public List<Backup> listBackup(String clusterId, String namespace) {
        List<BackupCRD> backupCRDList = backupWrapper.list(clusterId, namespace);
        if (CollectionUtils.isEmpty(backupCRDList)) {
            return new ArrayList<>(0);
        }

        List<Backup> backupList = new ArrayList<>();
        backupCRDList.forEach(backupCRD -> {
            Backup backup = new Backup().setName(backupCRD.getMetadata().getName())
                .setNamespace(backupCRD.getMetadata().getNamespace())
                .setControllerName(backupCRD.getMetadata().getLabels().get("controllername"))
                .setMiddlewareCluster(backupCRD.getSpec().getClusterName())
                .setBucketName(backupCRD.getSpec().getStorageProvider().getMinio().getBucketName())
                .setEndPoint(backupCRD.getSpec().getStorageProvider().getMinio().getEndpoint());
            if (!ObjectUtils.isEmpty(backupCRD.getStatus())) {
                backup.setBackupFileName(backupCRD.getStatus().getBackupFileName())
                    .setBackupTime(backupCRD.getStatus().getBackupTime()).setPhase(backupCRD.getStatus().getPhase());
            }
            backupList.add(backup);
        });

        return backupList;
    }

    /**
     * 创建备份
     *
     * @param backupCRD
     * @return
     */
    @Override
    public void create(String clusterId, BackupCRD backupCRD) {
        try {
            backupWrapper.create(clusterId, backupCRD);
        } catch (IOException e) {
            log.error("备份{}创建失败", backupCRD.getMetadata().getName());
            throw new CaasRuntimeException(ErrorMessage.CREATE_BACKUP_FAILED);
        }
    }

    /**
     * 删除备份cr
     *
     * @param clusterId
     * @param namespace
     * @param name
     * @return List<BackupDto>
     */
    @Override
    public void delete(String clusterId, String namespace, String name) throws Exception {
        try {
            backupWrapper.delete(clusterId, namespace, name);
        } catch (Exception e){
            log.error("删除备份失败", e);
            throw new CaasRuntimeException(ErrorMessage.DELETE_BACKUP_FAILED);
        }
    }

    /**
     * 获取备份文件信息（用于创建实例）
     *
     * @param middleware
     * @return BackupStorageProvider
     */
    @Override
    public BackupStorageProvider getStorageProvider(Middleware middleware) {
        List<BackupCRD> backupCRDList = backupWrapper.list(middleware.getClusterId(), middleware.getNamespace());

        backupCRDList = backupCRDList.stream()
            .filter(crd -> crd.getStatus() != null
                && StringUtils.equals(crd.getStatus().getBackupFileName(), middleware.getBackupFileName()))
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(backupCRDList)) {
            throw new CaasRuntimeException(ErrorMessage.BACKUP_FILE_NOT_EXIST);
        }

        BackupStorageProvider backupStorageProvider = backupCRDList.get(0).getSpec().getStorageProvider();
        backupStorageProvider.getMinio().setBackupFileName(middleware.getBackupFileName());
        return backupStorageProvider;
    }
}
