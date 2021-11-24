package com.harmonycloud.zeus.schedule;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.operator.BaseOperator;
import com.harmonycloud.zeus.operator.impl.MysqlOperatorImpl;
import com.harmonycloud.zeus.service.middleware.impl.MiddlewareBackupServiceImpl;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author dengyulong
 * @date 2021/04/14
 * 中间件管理的异步任务
 */
@Slf4j
@Component
public class MiddlewareManageTask {

    /**
     * 异步创建中间件
     *
     * @param middleware 中间件信息
     * @param cluster    集群
     * @param operator   operator实现
     */
    @Async("taskExecutor")
    public void asyncCreate(Middleware middleware, MiddlewareClusterDTO cluster, BaseOperator operator) {
        operator.create(middleware, cluster);
    }

    /**
     * 异步修改中间件
     */
    @Async("taskExecutor")
    public void asyncUpdate(Middleware middleware, MiddlewareClusterDTO cluster, BaseOperator operator) {
        operator.update(middleware, cluster);
    }

    /**
     * 异步删除中间件
     */
    @Async("taskExecutor")
    public void asyncDelete(Middleware middleware, BaseOperator operator) {
        operator.delete(middleware);
    }

    /**
     * 异步主从切换
     */
    @Async("taskExecutor")
    public void asyncSwitch(Middleware middleware, BaseOperator operator) {
        operator.switchMiddleware(middleware);
    }

    /**
     * 异步创建mysql灾备实例
     * @param mysqlOperator
     * @param middleware
     */
    @Async("singleThreadExecutor")
    public void asyncCreateDisasterRecoveryMiddleware(MysqlOperatorImpl mysqlOperator, Middleware middleware){
        mysqlOperator.createDisasterRecoveryMiddleware(middleware);
    }

    /**
     * 创建恢复
     * @param clusterId 集群id
     * @param namespace 分区
     * @param type 中间件类型
     * @param middlewareName 中间件名称
     * @param backupName 备份名称
     * @param restoreName 恢复记录名称
     * @param backupService  备份服务
     */
    @Async("singleThreadExecutor")
    public void asyncCreateBackupRestore(String clusterId, String namespace, String type, String middlewareName, String backupName, String restoreName, MiddlewareBackupServiceImpl backupService) {
        try {
            backupService.tryCreateMiddlewareRestore(clusterId, namespace, type, middlewareName, backupName, restoreName);
        } catch (Exception e) {
            log.error("通用备份，创建恢复失败 clusterId={},namespace={},type={},middlewareName={},backupName={},restoreName={}", clusterId, namespace, middlewareName, backupName, restoreName, e);
        }
    }

    /**
     * 中间件版本升级
     * @param middleware 中间件信息
     * @param currentValues 当前values
     * @param upgradeValues 升级values
     * @param upgradeChartVersion 升级chart版本
     * @param cluster 集群信息
     * @param helmChartService helmchart服务
     */
    @Async("taskExecutor")
    public void asyncUpdateChart(Middleware middleware, JSONObject currentValues, JSONObject upgradeValues, String upgradeChartVersion, MiddlewareClusterDTO cluster,HelmChartService helmChartService) {
        helmChartService.upgradeChart(middleware, currentValues, upgradeValues, upgradeChartVersion, cluster);
    }
}
