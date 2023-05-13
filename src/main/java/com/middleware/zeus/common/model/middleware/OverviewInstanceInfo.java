package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 总览中间件实例实体类
 * @author chenzhiling
 * @Date 2021/4/29 11:31 上午
 */
@ApiModel("总览中间件实例实体类")
//@Accessors(chain = true)
@Data
public class OverviewInstanceInfo {

    @ApiModelProperty("集群编号")
    private String clusterId;

    @ApiModelProperty("空间名称")
    private String namespace;

    @ApiModelProperty("实例名称")
    private String name;

    @ApiModelProperty("实例类型")
    private String type;

    @ApiModelProperty("chart包名称")
    private String chartName;

    @ApiModelProperty("chart包版本")
    private String chartVersion;

    @ApiModelProperty("image路径")
    private String imagePath;

    @ApiModelProperty("节点数")
    private Integer nodeCount = 0;

    @ApiModelProperty("总CPU占用")
    private Double totalCpu = 0.0d;

    @ApiModelProperty("总内存占用")
    private Double totalMemory = 0.0d;

    @ApiModelProperty("状态，false-异常；true-正常")
    private Boolean status = true;

}
