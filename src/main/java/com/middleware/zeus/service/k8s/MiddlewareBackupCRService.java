package com.middleware.zeus.service.k8s;

import com.middleware.zeus.integration.cluster.bean.MiddlewareBackup;

import java.io.IOException;
import java.util.List;
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
     * @param middlewareBackup
     * @throws IOException
     */
    void create(String clusterId, MiddlewareBackup middlewareBackup)  throws IOException;

    /**
     * 更新备份
     * @param clusterId
     * @param middlewareBackup
     * @throws IOException
     */
    void update(String clusterId, MiddlewareBackup middlewareBackup)  throws IOException;

    /**
     * 删除备份记录
     * @param clusterId
     * @param namespace
     * @param name
     * @throws IOException
     */
    void delete(String clusterId, String namespace,String name)  throws IOException;

    /**
     * 强制删除
     * @param clusterId
     * @param namespace
     * @param name
     * @param forceDelete
     * @throws IOException
     */
    void delete(String clusterId, String namespace, String name, Boolean forceDelete)  throws IOException;

    /**
     * 查询备份列表
     * @param clusterId
     * @param namespace
     * @param labels
     * @return
     */
    List<MiddlewareBackup> list(String clusterId, String namespace, Map<String,String> labels);

    /**
     * 查询备份列表
     * @param clusterId
     * @param namespace
     * @return
     */
    List<MiddlewareBackup> list(String clusterId, String namespace);

    /**
     * 根据备份名称查询备份
     * @param clusterId
     * @param namespace
     * @param name
     * @return
     */
    MiddlewareBackup get(String clusterId, String namespace, String name);
}
