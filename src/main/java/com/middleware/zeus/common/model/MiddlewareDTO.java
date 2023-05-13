package com.middleware.zeus.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author liyinlong
 * @since 2021-08-20 10:11
 */
@Data
@NoArgsConstructor
public class MiddlewareDTO implements Comparable<MiddlewareDTO>{

    /**
     * 集群名称
     */
    private String clusterName;

    /**
     * 集群id
     */
    private String clusterId;

    /**
     * 分区
     */
    private String namespace;

    /**
     * 分区可用cpu
     */
    private String namespaceCpu;

    /**
     * 分区可用内存
     */
    private String namespaceMemory;

    /**
     * 中间件名称
     */
    private String name;

    /**
     * 中间件类型
     */
    private String type;

    /**
     * 中间件所占cpu
     */
    private String cpu;

    /**
     * 中间件所占内存
     */
    private String memory;

    /**
     * 中间件状态
     */
    private String status;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 创建时间的时间戳
     */
    private Date createDate;

    /**
     * chart版本
     */
    private String chartVersion;

    /**
     * 是否为主备
     */
    private Boolean source;

    @Override
    public int compareTo(MiddlewareDTO o) {
        return o.getCreateDate().compareTo(createDate);
    }
}
