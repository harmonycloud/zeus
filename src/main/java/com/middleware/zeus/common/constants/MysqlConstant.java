package com.middleware.zeus.common.constants;

/**
 * @description mysql字段常量
 * @author  liyinlong
 * @since 2021/8/31 3:17 下午
 */
public class MysqlConstant {

    public static final String ARGS = "args";
    /**
     *  集群类型 master-master master-slave slave-slave
     */
    public static final String SPEC_TYPE = "type";
    /**
     * 副本数量
     */
    public static final String REPLICA_COUNT = "replicaCount";
    /**
     * 是否是源实例
     */
    public static final String IS_SOURCE = "isSource";
    /**
     * 灾备集群id
     */
    public static final String RELATION_CLUSTER_ID = "relationClusterId";
    /**
     * 灾备分区id
     */
    public static final String RELATION_NAMESPACE = "relationNamespace";
    /**
     * 灾备实例名称
     */
    public static final String RELATION_NAME = "relationName";
    /**
     * 灾备实例别名
     */
    public static final String RELATION_ALIAS_NAME = "relationAliasName";

    /**
     * chart名称
     */
    public static final String CHART_NAME = "chartName";

    /**
     * 创建mysql对外服务的失败重试次数
     */
    public static final int TIME_OF_RETRY_CREATE_SERVICE = 600;

    /**
     * mysql限定名字段
     */
    public static final String MYSQL_QUALIFIED_NAME = "mysql_qualified_name";

    /**
     * mysql用户名字段
     */
    public static final String USER = "user";

    /**
     * mysql数据库字段
     */
    public static final String DB = "db";

    /**
     * mysql特性设置 设置是否采集审计日志
     */
    public static final String KEY_FEATURES_AUDITLOG = "auditLog";
    /**
     * mysql慢日志
     */
    public static final String SLOW_QUERY_LOG = "slow_query_log";
}
