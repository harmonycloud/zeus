package com.middleware.zeus.service.middleware;

import com.middleware.caas.common.model.MiddlewareBackupDTO;
import com.middleware.caas.common.model.MiddlewareIncBackupDto;
import com.middleware.caas.common.model.middleware.MiddlewareBackupRecord;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/24
 */
public interface MiddlewareBackupService {

    /**
     * 创建备份(创建备份规则，或者立即备份)
     *
     * @param middlewareBackupDTO 备份信息
     * @return
     */
    void createBackup(MiddlewareBackupDTO middlewareBackupDTO);

    /**
     * 创建增量备份
     *
     * @param middlewareBackupDTO 备份信息
     */
    void createIncBackup(String clusterId, String namespace, String backupName, String time);

    /**
     * 更新备份规则
     *
     * @param middlewareBackupDTO
     * @return
     */
    void updateBackupSchedule(MiddlewareBackupDTO middlewareBackupDTO);

    /**
     * 创建备份规则
     *
     * @param backupDTO
     * @return
     */
    void createBackupSchedule(MiddlewareBackupDTO backupDTO);

    /**
     * 立即备份
     *
     * @param backupDTO
     * @return
     */
    void createNormalBackup(MiddlewareBackupDTO backupDTO);

    /**
     * 查询备份规则列表
     *
     * @param clusterId      集群id
     * @param namespace      分区
     * @param type           中间件类型
     * @param middlewareName 中间件名称
     * @return
     */
    List<MiddlewareBackupRecord> listBackupSchedule(String clusterId, String namespace, String type, String middlewareName);

    /**
     * 删除备份规则
     *
     * @param clusterId          集群id
     * @param namespace          分区
     * @param type
     * @param backupScheduleName 备份规则名称
     * @return
     */
    void deleteSchedule(String clusterId, String namespace, String type, String backupScheduleName);

    /**
     * 删除备份记录
     *
     * @param clusterId      集群id
     * @param namespace      分区
     * @param type           中间件类型
     * @param backupName     备份记录名称
     */
    void deleteRecord(String clusterId, String namespace, String type, String backupName);

    /**
     * 查询备份任务列表
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param type           中间件类型
     * @param middlewareName 中间件名称
     * @return
     */
    List<MiddlewareBackupRecord> listBackup(String clusterId, String namespace, String middlewareName, String type);

    /**
     * 创建恢复
     *
     * @param clusterId      集群id
     * @param namespace      分区
     * @param middlewareName 服务名称
     * @param type           服务类型
     * @param backupName     备份记录名称
     * @param restoreTime    恢复时间
     * @return
     */
    void createRestore(String clusterId, String namespace, String middlewareName, String type, String backupName, String restoreTime);

    /**
     * 删除中间件备份相关信息，包括定时备份、立即备份、备份恢复
     *
     * @param clusterId
     * @param namespace
     * @param type
     * @param middlewareName
     */
    void deleteMiddlewareBackupInfo(String clusterId, String namespace, String type, String middlewareName);

    /**
     * 检查中间件是否已创建备份规则
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @return
     */
    boolean checkIfAlreadyBackup(String clusterId, String namespace, String type, String middlewareName);

    /**
     * 检查中间件pod是否已创建备份规则
     * @param clusterId
     * @param namespace
     * @param type
     * @param middlewareName
     * @param podName
     * @return
     */
    boolean checkIfAlreadyBackup(String clusterId, String namespace, String type, String middlewareName,String podName);

    /**
     * 备份任务列表
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @param type
     * @return
     */
    List<MiddlewareBackupRecord> backupTaskList(String clusterId, String namespace, String middlewareName, String type, String keyword);

    /**
     * 备份任务详情
     * @param clusterId
     * @param namespace
     * @param backupName
     * @return
     */
    MiddlewareIncBackupDto getIncBackupInfo(String clusterId, String namespace, String backupName);

    /**
     * 备份记录
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @param type
     * @return
     */
    List<MiddlewareBackupRecord> backupRecords(String clusterId, String namespace, String backupName, String type);

    /**
     * 删除备份任务
     * @param clusterId
     * @param namespace
     * @param type
     * @param backupName
     * @param backupId
     * @param cron
     */
    void deleteBackUpTask(String clusterId, String namespace, String type, String backupName, String backupId, Boolean cron);

    /**
     * 删除备份记录
     * @param clusterId
     * @param namespace
     * @param type
     * @param backupName
     * @param backupId
     */
    void deleteBackUpRecord(String clusterId, String namespace, String type, String backupName, String backupId);

    /**
     * 创建备份任务名称映射信息
     * @param clusterId
     * @param taskName
     * @param backupId
     */
    void createBackupName(String clusterId, String taskName, String backupId, String backupType);

    /**
     * 删除备份任务名称映射信息
     * @param clusterId
     * @param taskName
     * @param backupType
     */
    void deleteBackupName(String clusterId, String taskName, String backupType);
}
