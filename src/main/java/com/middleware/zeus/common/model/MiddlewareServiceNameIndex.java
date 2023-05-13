package com.middleware.zeus.common.model;

import lombok.Data;

/**
 * 为中间件创建nodeport服务时的服务名称
 * @author liyinlong
 * @since 2021/9/8 3:59 下午
 */
@Data
public class MiddlewareServiceNameIndex {

    /***
     * 需要创建的NodePort服务的名称
     */
    private String nodePortServiceName;

    /**
     * 复用selector的中间件服务的后缀
     */
    private String middlewareServiceNameSuffix;

    public MiddlewareServiceNameIndex(String nodePortServiceName, String middlewareServiceNameSuffix) {
        this.nodePortServiceName = nodePortServiceName;
        this.middlewareServiceNameSuffix = middlewareServiceNameSuffix;
    }
}
