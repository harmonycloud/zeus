package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.MiddlewareBackupDTO;
import com.harmonycloud.caas.common.model.MiddlewareBackupDetail;
import com.harmonycloud.caas.common.model.middleware.MiddlewareBackupRecord;
import io.fabric8.kubernetes.api.model.OwnerReference;

import java.util.List;
import java.util.Map;

/**
 * @author dengyulong
 * @date 2021/03/24
 */
public interface MiddlewareBackupService {

    /**
     * 查询备份数据列表
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param type           中间件类型
     * @param middlewareName 中间件名称
     * @return
     */
    List<MiddlewareBackupRecord> list(String clusterId, String namespace, String middlewareName, String type);

    /**
     * 创建备份
     *
     * @param middlewareBackupDTO 备份信息
     * @return
     */
    BaseResult create(MiddlewareBackupDTO middlewareBackupDTO);

    /**
     * 更新备份配置
     *
     * @param middlewareBackupDTO
     * @return
     */
    BaseResult update(MiddlewareBackupDTO middlewareBackupDTO);

    /**
     * 删除备份记录
     * @param clusterId
     * @param namespace
     * @param middlewareName
     * @param type
     * @param backupName
     * @param backupFileName
     * @return
     */
    BaseResult delete(String clusterId, String namespace,String middlewareName, String type, String backupName, String backupFileName);

    /**
     * 查询备份设置
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param type           中间件类型
     * @param middlewareName 中间件名称
     * @return
     */
    BaseResult get(String clusterId, String namespace, String middlewareName, String type);

    /**
     * 创建定时备份
     *
     * @param detail
     * @return
     */
    BaseResult createScheduleBackup(MiddlewareBackupDetail detail);

    /**
     * 立即备份
     *
     * @param detail
     * @return
     */
    BaseResult createNormalBackup(MiddlewareBackupDetail detail);

    /**
     * 创建恢复
     *
     * @param clusterId
     * @param namespace
     * @param backupName 恢复服务名称
     * @param aliasName
     * @return
     */
    BaseResult createRestore(String clusterId, String namespace, String middlewareName, String type, String restoreName, String backupName, String backupFileName, String aliasName);

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
     * @param clusterId
     * @param namespace
     * @param type
     * @param middlewareName
     */
    void deleteMiddlewareBackupInfo(String clusterId, String namespace, String type, String middlewareName);
}
