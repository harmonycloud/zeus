package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupScheduleCRD;

import java.io.IOException;

/**
 * 中间件备份
 * @author  liyinlong
 * @since 2021/9/14 10:43 上午
 */
public interface MiddlewareBackupScheduleCRDService {

    /**
     * 创建备份
     * @param clusterId
     * @param middlewareBackupScheduleCRD
     * @throws IOException
     */
    void create(String clusterId, MiddlewareBackupScheduleCRD middlewareBackupScheduleCRD)  throws IOException;

    /**
     * 更新备份
     * @param clusterId
     * @param middlewareBackupScheduleCRD
     * @throws IOException
     */
    void update(String clusterId, MiddlewareBackupScheduleCRD middlewareBackupScheduleCRD)  throws IOException;

    /**
     * 查询备份
     * @param clusterId
     * @param namespace
     * @param backupName
     * @return
     */
    MiddlewareBackupScheduleCRD get(String clusterId, String namespace, String backupName);

}
