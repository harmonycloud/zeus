package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 总览分区实体类
 * @author chenzhiling
 * @Date 2021/4/29 11:31 上午
 */
@ApiModel("总览分区实体类")
@Accessors(chain = true)
@Data
public class OverviewNamespaceInfo extends Namespace {

    @ApiModelProperty("中间件实例数")
    private Integer instanceCount = 0;

    @ApiModelProperty("中间件实例异常数")
    private Integer instanceExceptionCount = 0;

    @ApiModelProperty("CPU配额")
    private Double cpu = 0.0d;

    @ApiModelProperty("内存配额")
    private Double memory = 0.0d;

    @ApiModelProperty("中间件详细")
    private List<OverviewInstanceInfo> middlewares;

    @ApiModelProperty("状态，false-异常；true-正常")
    private Boolean status = true;

}
