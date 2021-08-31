package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.middleware.ScheduleBackup;
import com.harmonycloud.zeus.integration.cluster.bean.ScheduleBackupCRD;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/4/7 2:03 下午
 */
public interface ScheduleBackupService {

    /**
     * 查询备份列表
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name      中间件名称
     * @return List<BackupDto>
     */
    List<ScheduleBackup> listScheduleBackup(String clusterId, String namespace, String name);

    /**
     * 创建定时备份
     *
     * @param clusterId         集群id
     * @param scheduleBackupCRD 定时备份信息
     * @return
     */
    void create(String clusterId, ScheduleBackupCRD scheduleBackupCRD);

    /**
     * 删除定时备份
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param name      中间件名称
     */
    void delete(String clusterId, String namespace, String name);

}
