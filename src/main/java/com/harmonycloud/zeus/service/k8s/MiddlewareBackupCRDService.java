package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.middleware.MiddlewareBackup;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupCRD;
import com.harmonycloud.zeus.integration.cluster.bean.MysqlReplicateCRD;

import java.io.IOException;

/**
 * 中间件备份
 * @author  liyinlong
 * @since 2021/9/14 10:43 上午
 */
public interface MiddlewareBackupCRDService {

    /**
     * 创建备份
     * @param clusterId
     * @param middlewareBackupCRD
     * @throws IOException
     */
    void create(String clusterId, MiddlewareBackupCRD middlewareBackupCRD)  throws IOException;

    void update(String clusterId, MiddlewareBackupCRD middlewareBackupCRD)  throws IOException;

    /**
     * 查询备份
     * @param clusterId
     * @param namespace
     * @param backupName
     * @return
     */
    MiddlewareBackupCRD get(String clusterId,String namespace,String backupName);
}
