package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.MiddlewareBackupDTO;
import com.harmonycloud.caas.common.model.MiddlewareBackupScheduleConfig;
import com.harmonycloud.caas.common.model.middleware.MiddlewareBackupRecord;

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
    BaseResult createBackup(MiddlewareBackupDTO middlewareBackupDTO);

    /**
     * 更新备份规则
     *
     * @param middlewareBackupDTO
     * @return
     */
    BaseResult updateBackupSchedule(MiddlewareBackupDTO middlewareBackupDTO);

    /**
     * 创建备份规则
     *
     * @param backupDTO
     * @return
     */
    BaseResult createBackupSchedule(MiddlewareBackupDTO backupDTO);

    /**
     * 立即备份
     *
     * @param backupDTO
     * @return
     */
    BaseResult createNormalBackup(MiddlewareBackupDTO backupDTO);

    /**
     * 查询备份规则列表
     *
     * @param clusterId      集群id
     * @param namespace      分区
     * @param type           中间件类型
     * @param middlewareName 中间件名称
     * @return
     */
    List<MiddlewareBackupScheduleConfig> listBackupSchedule(String clusterId, String namespace, String type, String middlewareName);

    /**
     * 删除备份规则
     *
     * @param clusterId          集群id
     * @param namespace          分区
     * @param type
     * @param backupScheduleName 备份规则名称
     * @return
     */
    BaseResult deleteSchedule(String clusterId, String namespace, String type, String backupScheduleName);

    /**
     * 删除备份记录
     *
     * @param clusterId      集群id
     * @param namespace      分区
     * @param middlewareName 中间件名称
     * @param type           中间件类型
     * @param backupName     备份记录名称
     * @param backupFileName 备份文件名称(mysql特有)
     * @return
     */
    BaseResult deleteRecord(String clusterId, String namespace, String middlewareName, String type, String backupName, String backupFileName);

    /**
     * 查询备份记录列表
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param type           中间件类型
     * @param middlewareName 中间件名称
     * @return
     */
    List<MiddlewareBackupRecord> listRecord(String clusterId, String namespace, String middlewareName, String type);

    /**
     * 创建恢复
     *
     * @param clusterId      集群id
     * @param namespace      分区
     * @param middlewareName 服务名称
     * @param type           服务类型
     * @param backupName     备份记录名称
     * @param backupFileName 备份文件名称
     * @param pods           pod列表
     * @return
     */
    BaseResult createRestore(String clusterId, String namespace, String middlewareName, String type, String backupName, String backupFileName, List<String> pods);

    /**
     * 尝试创建中间件恢复实例
     *
     * @param clusterId      集群id
     * @param namespace      分区
     * @param type           中间件类型
     * @param middlewareName 中间件名称
     * @param backupName     备份名称
     * @param restoreName    恢复中间件名称
     */
    void tryCreateMiddlewareRestore(String clusterId, String namespace, String type, String middlewareName, String backupName, String restoreName);

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

}
