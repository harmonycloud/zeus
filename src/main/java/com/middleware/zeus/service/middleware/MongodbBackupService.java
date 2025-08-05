package com.middleware.zeus.service.middleware;

import com.middleware.zeus.common.model.middleware.mongodb.MongodbBackupRecordDo;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbScheduleBackupDto;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbBackupServerDto;

/**
 * @author xutianhong
 * @Date 2025/7/31 10:06
 */
public interface MongodbBackupService {

    /**
     * mongodb 配置备份服务器地址
     *
     * @param mongodbBackupServerDto mongodb备份服务器地址
     */
    void updateBackupServer(MongodbBackupServerDto mongodbBackupServerDto);

    /**
     * mongodb 获取备份服务器地址
     *
     * @param clusterId 集群id
     */
    MongodbBackupServerDto getBackupServer(String clusterId);

    /**
     * mongodb 设置开启周期备份
     *
     * @param mongodbScheduleBackupDto mongodb备份
     */
    void enableScheduleBackup(MongodbScheduleBackupDto mongodbScheduleBackupDto);

    /**
     * mongodb 设置开启周期备份
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name 中间件名称
     */
    void disableScheduleBackup(String clusterId, String namespace, String name);

    /**
     * mongodb 查询周期备份配置
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name 中间件名称
     */
    MongodbScheduleBackupDto getScheduleBackup(String clusterId, String namespace, String name);

    /**
     * mongodb 执行单词备份
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name 中间件名称
     * @param retentionDays 保留时间
     */
    void singleBackup(String clusterId, String namespace, String name, Integer retentionDays);

    /**
     * mongodb 备份记录查询
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name 中间件名称
     */
    MongodbBackupRecordDo backupList(String clusterId, String namespace, String name);

    /**
     * mongodb 创建恢复
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name 中间件名称
     */
    void createRestore(String clusterId, String namespace, String name, String snapshotId);

}
