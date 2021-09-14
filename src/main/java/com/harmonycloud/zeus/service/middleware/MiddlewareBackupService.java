package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.MiddlewareBackup;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareBackupSpec;

import java.io.IOException;
import java.util.List;

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
    List list(String clusterId, String namespace, String type, String middlewareName);

    /**
     * 创建备份
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param type           中间件类型
     * @param middlewareName 中间件名称
     * @return
     */
    BaseResult create(String clusterId, String namespace, String type, String middlewareName, String cron, Integer limitRecord);

    /**
     * 更新备份配置
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param type           中间件类型
     * @param middlewareName 中间件名称
     * @return
     */
    BaseResult update(String clusterId, String namespace, String type, String middlewareName, String cron, Integer limitRecord);

    /**
     * 查询备份设置
     * @param clusterId
     * @param namespace
     * @param type
     * @param middlewareName
     * @return
     */
    BaseResult get(String clusterId, String namespace, String type, String middlewareName);

}
