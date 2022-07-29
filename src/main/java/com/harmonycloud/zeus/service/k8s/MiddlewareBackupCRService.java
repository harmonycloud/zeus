package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupCR;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupList;

import java.io.IOException;
import java.util.Map;

/**
 * 中间件备份记录
 * @author  liyinlong
 * @since 2021/9/14 10:43 上午
 */
public interface MiddlewareBackupCRService {

    /**
     * 创建备份
     * @param clusterId
     * @param middlewareBackupCR
     * @throws IOException
     */
    void create(String clusterId, MiddlewareBackupCR middlewareBackupCR)  throws IOException;

    /**
     * 删除备份记录
     * @param clusterId
     * @param namespace
     * @param name
     * @throws IOException
     */
    void delete(String clusterId, String namespace,String name)  throws IOException;

    /**
     * 查询备份列表
     * @param clusterId
     * @param namespace
     * @param labels
     * @return
     */
    MiddlewareBackupList list(String clusterId, String namespace, Map<String,String> labels);

    /**
     * 查询备份列表
     * @param clusterId
     * @param namespace
     * @return
     */
    MiddlewareBackupList list(String clusterId, String namespace);

    /**
     * 根据备份名称查询备份
     * @param clusterId
     * @param namespace
     * @param name
     * @return
     */
    MiddlewareBackupCR get(String clusterId, String namespace, String name);
}
