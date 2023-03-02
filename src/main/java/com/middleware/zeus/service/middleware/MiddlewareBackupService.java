package com.middleware.zeus.service.middleware;

import com.middleware.caas.common.model.MiddlewareBackupDTO;
import com.middleware.caas.common.model.MiddlewareIncBackup;
import com.middleware.caas.common.model.MiddlewareIncBackupDto;
import com.middleware.caas.common.model.MiddlewareTaskDTO;
import com.middleware.caas.common.model.middleware.MiddlewareBackupRecord;
import com.middleware.caas.common.model.middleware.MiddlewareBackupRecordGroup;
import com.middleware.zeus.integration.cluster.bean.MiddlewareBackupScheduleCR;
import com.middleware.zeus.integration.cluster.bean.Minio;
import io.fabric8.kubernetes.api.model.ObjectMeta;

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
     * 创建增量备份
     *
     */
    void createIncBackup(String clusterId, String namespace, String backupName, String time);

    /**
     * 创建增量备份
     *
     */
    void createIncBackup(String clusterId, String namespace, String backupName, String time, MiddlewareBackupScheduleCR scheduleCR);

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
    void createIncBackupSchedule(MiddlewareIncBackup middlewareIncBackup, ObjectMeta objectMeta);

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
     * 查询备份任务详情
     * @param clusterId
     * @param namespace
     * @param backupName 备份任务名称
     * @param backupMode 备份任务类型 period:周期备份 single:单次备份
     * @return
     */
    MiddlewareBackupRecord getBackup(String clusterId, String namespace,String backupName,String backupMode);

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
     * 获取备份任务组列表
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @param projectId
     * @param type
     * @param keyword
     * @return
     */
    List<MiddlewareBackupRecordGroup> backupTaskGroupList(String clusterId, String namespace, String middlewareName, String projectId, String type, String keyword);

    /**
     * 备份任务详情
     * @param clusterId
     * @param namespace
     * @param backupName
     * @return
     */
    MiddlewareIncBackupDto getIncBackupInfo(String clusterId, String namespace, String backupName);

    /**
     * 查询备份记录列表
     * @param clusterId
     * @param namespace
     * @param backupName
     * @param type
     * @return
     */
    List<MiddlewareBackupRecord> backupRecords(String clusterId, String namespace, String backupName, String type);

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
     */
    void deleteBackUpRecord(String clusterId, String namespace, String type, String backupName, String backupId);

    /**
     * 创建备份任务名称映射信息
     * @param clusterId
     * @param taskName
     * @param backupId
     * @param positionId
     */
    void saveBackupName(String clusterId, String taskName, String backupId, String backupType, Integer positionId);

    /**
     * 根据备份位置id查询备份任务(含单次备份和周期备份)
     * @param clusterId
     * @param namespace
     * @param labels
     * @return
     */
    List<MiddlewareBackupRecord> listBackupTask(String clusterId, String namespace, Map<String, String> labels);

    /**
     * 检查中间件是否已创建周期备份任务
     * @param clusterId
     * @param namespace
     * @param type
     * @param middlewareName
     * @return
     */
    boolean checkSchedule(String clusterId, String namespace, String type, String middlewareName);

}
