package com.middleware.zeus.service.k8s;

import com.middleware.zeus.integration.cluster.bean.MiddlewareBackupSchedule;
import com.middleware.zeus.integration.cluster.bean.MiddlewareBackupScheduleList;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 中间件备份
 * @author  liyinlong
 * @since 2021/9/14 10:43 上午
 */
public interface MiddlewareBackupScheduleCRDService {

    /**
     * 创建备份
     * @param clusterId
     * @param middlewareBackupSchedule
     * @throws IOException
     */
    void create(String clusterId, MiddlewareBackupSchedule middlewareBackupSchedule)  throws IOException;

    /**
     * 创建备份
     * @param clusterId
     * @param middlewareBackupSchedule
     * @throws IOException
     */
    void createOrReplace(String clusterId, MiddlewareBackupSchedule middlewareBackupSchedule)  throws IOException;

    /**
     * 更新备份
     * @param clusterId
     * @param middlewareBackupSchedule
     * @throws IOException
     */
    void update(String clusterId, MiddlewareBackupSchedule middlewareBackupSchedule)  throws IOException;

    /**
     * 查询备份
     * @param clusterId
     * @param namespace
     * @param backupName
     * @return
     */
    MiddlewareBackupSchedule get(String clusterId, String namespace, String backupName);

    /**
     * 删除定时备份
     * @param clusterId
     * @param namespace
     * @param name
     * @throws IOException
     */
    void delete(String clusterId, String namespace, String name)  throws IOException;

    /**
     *
     * @param clusterId
     * @param namespace
     * @return
     */
    MiddlewareBackupScheduleList list(String clusterId, String namespace);

    /**
     * 根据标签查询定时备份任务
     *
     * @param clusterId 集群id
     * @param namespace 分区
     * @param labels    标签
     * @return MiddlewareBackupScheduleList
     */
    List<MiddlewareBackupSchedule> listByLabels(String clusterId, String namespace, Map<String, String> labels);
}
