package com.middleware.zeus.common.model.middleware;

import lombok.Data;

import java.util.List;

/**
 * 中间件控制器
 * @author liyinlong
 * @since 2021/9/24 10:30 上午
 */
@Data
public class MiddlewareOperatorDTO {

    /**
     * 控制器列表
     */
    private List<MiddlewareOperator> operatorList;

    /**
     * 正常控制器数量
     */
    private int running;

    /**
     * 正常控制器百分比
     */
    private String runningPercent;

    /**
     * 异常控制器数量
     */
    private int error;

    /**
     * 异常控制器所占百分比
     */
    private String errorPercent;

    /**
     * 控制器总数
     */
    private int total;

    @Data
    public static class MiddlewareOperator{
        /**
         * 控制器名称
         */
        private String name;

        /**
         * 控制器状态 1：正常 3：异常
         */
        private int status;

        /**
         * 集群名称
         */
        private String clusterName;

        /**
         * 集群id
         */
        private String clusterId;
    }
}
