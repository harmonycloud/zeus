package com.harmonycloud.zeus.service.middleware;

import java.util.List;

import com.harmonycloud.caas.common.model.middleware.Backup;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.integration.cluster.bean.BackupCR;
import com.harmonycloud.zeus.integration.cluster.bean.BackupStorageProvider;

/**
 * @author xutianhong
 * @Date 2021/4/2 4:24 下午
 */
public interface BackupService {

    /**
     * 查询备份列表
     *
     * @param clusterId
     * @param namespace
     * @return List<BackupDto>
     */
    List<Backup> listBackup(String clusterId, String namespace);

    /**
     * 创建备份
     *
     * @param backupCR
     * @return
     */
    void create(String clusterId, BackupCR backupCR);

    /**
     * 删除备份cr
     *
     * @param clusterId
     * @param namespace
     * @param name
     * @return List<BackupDto>
     */
    void delete(String clusterId, String namespace, String name) throws Exception;

    /**
     * 获取备份文件信息（用于创建实例）
     *
     * @param middleware
     * @return BackupStorageProvider
     */
    BackupStorageProvider getStorageProvider(Middleware middleware);

    /**
     * 获取定时任务产生的backup
     * @param clusterId
     * @param namespace
     * @param backupName
     * @return
     */
    List<Backup> listScheduleBackup(String clusterId, String namespace, String backupName);

}
