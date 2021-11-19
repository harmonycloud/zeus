package com.harmonycloud.zeus.operator.api;

import com.harmonycloud.zeus.operator.BaseOperator;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public interface MysqlOperator extends BaseOperator {

    /**
     * 灾备切换
     * @param clusterId 集群id
     * @param namespace 分区名称
     * @param middlewareName 中间件名称
     * @throws Exception
     */
    void switchDisasterRecovery(String clusterId, String namespace, String middlewareName) throws Exception;

}
