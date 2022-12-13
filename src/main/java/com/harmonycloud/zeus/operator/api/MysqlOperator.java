package com.harmonycloud.zeus.operator.api;

import com.middleware.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.operator.BaseOperator;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public interface MysqlOperator extends BaseOperator {

    /**
     * 灾备切换
     *
     * @param clusterId      集群id
     * @param namespace      分区名称
     * @param middlewareName 中间件名称
     * @throws Exception
     */
    void switchDisasterRecovery(String clusterId, String namespace, String middlewareName) throws Exception;

    /**
     * 为mysql创建一个对外服务
     *
     * @param middleware
     */
    void prepareDbManageOpenService(Middleware middleware);

    /**
     * 创建灾备实例
     *
     * @param middleware
     */
    void createDisasterRecoveryMiddleware(Middleware middleware);

    /**
     * @param original
     * @param disasterRecovery
     */
    void createMysqlReplicate(Middleware original, Middleware disasterRecovery);

    /**
     * @param middleware
     */
    void createOpenService(Middleware middleware, boolean createReadOnlyService, boolean useNodePort);

    /**
     * 为mysql创建一个ingress service
     *
     * @param middleware
     */
    void createIngressService(Middleware middleware, boolean createReadOnlyService);

    /**
     * 删除灾备关联关系和关联信息
     *
     * @param middleware
     */
    void deleteDisasterRecoveryInfo(Middleware middleware);

    /**
     * 清除数据库管理可能存在的脏数据，并保存root用户信息到数据库
     *
     * @param middleware
     */
    void prepareDbManageEnv(Middleware middleware);

    /**
     * 清除可能残留的已发布同名服务的数据库、用户、数据授权信息
     *
     * @param middleware
     */
    void clearDbManageData(Middleware middleware);
}
