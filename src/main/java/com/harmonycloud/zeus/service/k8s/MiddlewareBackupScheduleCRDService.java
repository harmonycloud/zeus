package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupScheduleCR;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupScheduleList;

import java.io.IOException;
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
     * @param middlewareBackupScheduleCR
     * @throws IOException
     */
    void create(String clusterId, MiddlewareBackupScheduleCR middlewareBackupScheduleCR)  throws IOException;

    /**
     * 更新备份
     * @param clusterId
     * @param middlewareBackupScheduleCR
     * @throws IOException
     */
    void update(String clusterId, MiddlewareBackupScheduleCR middlewareBackupScheduleCR)  throws IOException;

    /**
     * 查询备份
     * @param clusterId
     * @param namespace
     * @param backupName
     * @return
     */
    MiddlewareBackupScheduleCR get(String clusterId, String namespace, String backupName);

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
}
