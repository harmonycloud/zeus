package com.harmonycloud.zeus.operator.api;

import java.util.List;

import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MysqlBackupDto;
import com.harmonycloud.caas.common.model.middleware.ScheduleBackupConfig;
import com.harmonycloud.zeus.operator.BaseOperator;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public interface MysqlOperator extends BaseOperator {

    /**
     * 查询备份列表
     *
     * @param middleware 中间件信息
     * @return
     */
    List<MysqlBackupDto> listBackups(Middleware middleware);

    /**
     * 查询定时备份配置
     *
     * @param middleware 中间件信息
     * @return ScheduleBackupConfig
     */
    ScheduleBackupConfig getScheduleBackupConfig(Middleware middleware);

    /**
     * 创建定时备份
     *
     * @param middleware  中间件信息
     * @param keepBackups 备份数量
     * @param cron cron表达式
     * @return
     */
    void createScheduleBackup(Middleware middleware, Integer keepBackups, String cron);

    /**
     * 创建备份
     *
     * @param middleware 中间件信息
     * @return
     */
    void createBackup(Middleware middleware);

    /**
     * 删除备份文件
     *
     * @param middleware     中间件信息
     * @param backupFileName 备份文件名称
     * @param backupName     备份名称
     */
    void deleteBackup(Middleware middleware, String backupFileName, String backupName) throws Exception;

    /**
     * 灾备切换
     * @param clusterId 集群id
     * @param namespace 分区名称
     * @param middlewareName 中间件名称
     * @throws Exception
     */
    void switchDisasterRecovery(String clusterId, String namespace, String middlewareName) throws Exception;

}
