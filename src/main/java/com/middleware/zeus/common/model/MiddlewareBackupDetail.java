package com.middleware.zeus.common.model;

import lombok.Data;

import java.util.Map;

/**
 * @author liyinlong
 * @since 2021/11/5 3:24 下午
 */
@Data
public class MiddlewareBackupDetail {

    /**
     * 中间件crd类型 例：mysqlcluster、kafkacluster
     */
    private String crdType;

    /**
     * 中间件全名 例：rediscluster-iredis
     */
    private String middlewareRealName;

    /**
     * 中间件备份的label
     */
    Map<String, String> labels;

    public MiddlewareBackupDetail() {
        super();
    }
}
