package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.base.BaseResult;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public interface MysqlService {

    /**
     * 灾备切换
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     */
    BaseResult switchDisasterRecovery(String clusterId, String namespace, String middlewareName);

    /**
     * 查询mysql访问信息
     *
     * @param clusterId      集群id
     * @param namespace      命名空间
     * @param middlewareName 中间件名称
     */
    BaseResult queryAccessInfo(String clusterId, String namespace, String middlewareName);

}
