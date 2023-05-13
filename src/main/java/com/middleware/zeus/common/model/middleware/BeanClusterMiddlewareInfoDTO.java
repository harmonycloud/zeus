package com.middleware.zeus.common.model.middleware;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author yushuaikang
 * @date 2022/1/6 下午3:21
 */
@Data
@Accessors(chain = true)
public class BeanClusterMiddlewareInfoDTO {

    /**
     * 自增id
     */
    private Integer id;

    /**
     * 集群id
     */
    private String clusterId;

    /**
     * chart名称
     */
    private String chartName;

    /**
     * chart版本
     */
    private String chartVersion;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 实例数
     */
    private int replicas;
}
