package com.middleware.zeus.service.middleware;

import com.middleware.zeus.common.model.*;
import com.middleware.zeus.common.model.middleware.*;
import com.middleware.zeus.integration.cluster.bean.MiddlewareBackupSchedule;
import com.middleware.zeus.integration.cluster.bean.Minio;
import io.fabric8.kubernetes.api.model.ObjectMeta;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
     * 保存增量备份
     *
     */
    void createOrReplaceIncBackup(String clusterId, String namespace, String backupId, String backupName, String time, String pause, Boolean sameActiveActiveBackup);

    /**
     * 保存增量备份
     *
     */
    void createOrReplaceIncBackup(String clusterId, String namespace, String backupName, String time, String pause);

    /**
     * 保存增量备份
     *
     */
    void createOrReplaceIncBackup(String clusterId, String namespace, String backupName, String time, String pause, MiddlewareBackupSchedule scheduleCR);

    /**
     * 更新备份周期备份
     * @param middlewareBackupDTO
     * @return
     */
    void updateBackupSchedule(MiddlewareBackupDTO middlewareBackupDTO);

    /**
     * 更新备份周期备份
     * @param backupName
     * @param middlewareBackupDTO
     */
    void updateBackupSchedule(String backupName, MiddlewareBackupDTO middlewareBackupDTO);

    /**
     * 创建备份规则
     *
     * @param backupDTO
     * @param minio
     * @param objectMeta
     * @return
     */
    void createBackupSchedule(MiddlewareBackupDTO backupDTO, Minio minio, ObjectMeta objectMeta);

    /**
     * 立即备份
     *
     * @param backupDTO
     * @param minio
     * @param objectMeta
     * @return
     */
    void createNormalBackup(MiddlewareBackupDTO backupDTO, Minio minio, ObjectMeta objectMeta);

    /**
     * 创建增量备份任务
     * @param middlewareIncBackup
     * @param objectMeta
     */
    void createOrReplaceIncBackupSchedule(MiddlewareIncBackup middlewareIncBackup, ObjectMeta objectMeta);

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
     * 删除备份规则
     * @param clusterId
     * @param namespace
     * @param type
     * @param backupScheduleName
     * @param forceDelete
     */
    void deleteSchedule(String clusterId, String namespace, String type, String backupScheduleName, Boolean forceDelete);

    /**
     * 删除备份记录
     *  @param clusterId      集群id
     * @param namespace      分区
     * @param type           中间件类型
     * @param backupName     备份记录名称
     * @param forceDelete
     */
    void deleteRecord(String clusterId, String namespace, String type, String backupName, Boolean forceDelete);

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
     * 查询备份任务详情
     * @param clusterId
     * @param namespace
     * @param backupId 备份任务id
     * @param backupMode 备份任务类型 period:周期备份 single:单次备份
     * @return
     */
    List<MiddlewareBackupRecord> getBackup(String clusterId, String namespace, String backupId, String backupMode);

    /**
     * 创建恢复
     *
     * @return
     */
    void createRestore(MiddlewareRestoreDto restoreDto);

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
     * 获取备份任务组列表
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @param projectId
     * @param type
     * @param keyword
     * @return
     */
    List<MiddlewareBackupRecordGroup> backupTaskGroupList(String clusterId, String namespace, String middlewareName, String organId, String projectId, String type, String keyword);

    /**
     * 获取增量备份任务详情
     * @param clusterId
     * @param namespace
     * @param backupName
     * @return
     */
    MiddlewareIncBackupDto getIncBackupInfo(String clusterId, String namespace, String backupName);

    /**
     * 获取增量备份任务详情列表
     * @param clusterId
     * @param namespace
     * @param backupId
     * @return
     */
    List<MiddlewareIncBackupDto> getIncBackupInfoList(String clusterId, String namespace, String backupId);

    /**
     * 查询全量备份记录
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @param type
     * @param backupId
     * @param backupMode
     * @param activeArea
     * @return
     */
    List<MiddlewareBackupRecord> backupRecords(String clusterId, String namespace, String middlewareName, String type, String backupId, String backupMode, String orderBy, String activeArea);

    /**
     * 获取备份进度
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @param backupName
     * @return
     */
    ProgressInfo getBackupProgress(String clusterId, String namespace, String middlewareName, String backupName);

    /**
     * 查询增量备份记录
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @param type
     * @param backupId
     * @param backupMode
     * @param orderBy
     * @return
     */
    List<MiddlewareBackupRecord> backupIncrRecords(String clusterId, String namespace, String middlewareName, String type,
                                                   String backupId, String backupMode, String orderBy);


    /**
     * 查询恢复记录
     * @param clusterId
     * @param namespace
     * @param backupId
     * @return
     */
    List<MiddlewareBackupRestore> backupRestores(String clusterId, String namespace, String backupId);

    /**
     * 查询恢复记录详情
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @param restoreName
     * @return
     */
    ProgressInfo getRestoreProgress(String clusterId, String namespace, String middlewareName, String restoreName);


    /**
     * 删除恢复记录
     * @param clusterId
     * @param namespace
     * @param restoreName
     * @param forceDelete 是否强制删除
     */
    void deleteRestoreRecord(String clusterId, String namespace, String restoreName, Boolean forceDelete);

    /**
     * 删除备份任务
     * @param taskDTO
     */
    void deleteBackUpTask(MiddlewareTaskDTO taskDTO);

    /**
     * 删除备份记录
     * @param clusterId
     * @param namespace
     * @param type
     * @param backupName
     * @param backupId
     * @param forceDelete
     */
    void deleteBackUpRecord(String clusterId, String namespace, String type, String backupName, String backupId, Boolean forceDelete);

    /**
     * 创建备份任务名称映射信息
     * @param clusterId
     * @param taskName
     * @param backupId
     * @param positionId
     */
    void saveBackupName(String clusterId, String taskName, String backupId, String backupType, Integer positionId);

    /**
     * 检查中间件是否已创建周期备份任务
     * @param clusterId
     * @param namespace
     * @param type
     * @param middlewareName
     * @return
     */
    boolean checkSchedule(String clusterId, String namespace, String type, String middlewareName);

    /**
     * 开启/禁用周期备份任务
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param backupId 备份任务id
     * @param enable true:开启 false:禁用
     */
    void enableBackup(String clusterId, String namespace, String middlewareName, String type, String backupId, Boolean enable);

    /**
     * 获取备份任务可恢复时间
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param backupId 备份任务id
     * @param dateStr 指定时间
     * @return BackupRestoreTimeDto 可恢复时间
     */
    BackupRestoreTimeDto restoreTime(String clusterId, String namespace, String backupId, String dateStr);

    /**
     * 检查增量备份任务开关
     */
    void checkSchedule();

}
