package com.middleware.zeus.common.model.middleware;

/**
 * 总览集群实体类
 * @author chenzhiling
 * @Date 2021/4/29 11:41 下午
 */

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 总览集群实体类
 * @author chenzhiling
 * @Date 2021/4/29 11:31 上午
 */
@ApiModel("总览集群实体类")
@Accessors(chain = true)
@Data
public class OverviewClusterInfo {

    @ApiModelProperty("集群编号")
    private String clusterId;

    @ApiModelProperty("集群名称")
    private String clusterName;

    @ApiModelProperty("注册命名空间数")
    private Integer regNamespaceCount = 0;

    @ApiModelProperty("中间件实例数")
    private Integer instanceCount = 0;

    @ApiModelProperty("状态，false-异常；true-正常")
    private Boolean status = true;

    @ApiModelProperty("namespace详细")
    private List<OverviewNamespaceInfo> namespaces;

}
