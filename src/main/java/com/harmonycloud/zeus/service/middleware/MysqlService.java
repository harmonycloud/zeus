package com.harmonycloud.zeus.service.middleware;

import java.util.List;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.MysqlBackupDto;
import com.harmonycloud.caas.common.model.middleware.ScheduleBackupConfig;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public interface MysqlService {

    /**
     * 查询备份列表
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @return
     */
    List<MysqlBackupDto> listBackups(String clusterId, String namespace, String middlewareName);

    /**
     * 查询定时备份配置
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @return
     */
    ScheduleBackupConfig getScheduleBackups(String clusterId, String namespace, String middlewareName);

    /**
     * 创建定时备份
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param keepBackups    备份数量
     * @param cron           cron表达式
     * @return
     */
    void createScheduleBackup(String clusterId, String namespace, String middlewareName,Integer keepBackups, String cron);

    /**
     * 创建备份
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @return
     */
    void createBackup(String clusterId, String namespace, String middlewareName);

    /**
     * 删除备份
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     * @param backupFileName 备份文件名称
     * @param backupName     备份名称
     * @return
     */
    void deleteBackup(String clusterId, String namespace, String middlewareName, String backupFileName, String backupName) throws Exception;

    /**
     * 灾备切换
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     */
    BaseResult switchDisasterRecovery(String clusterId, String namespace, String middlewareName);

    /**
     * 灾备切换
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     */
    BaseResult queryAccessInfo(String clusterId, String namespace, String middlewareName);
}
