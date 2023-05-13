package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2020/12/16 4:55 下午
 */

@Accessors(chain = true)
@Data
@ApiModel(description = "镜像版本保留策略执行结果详情项")
public class RetentionExecutionItem {
    @ApiModelProperty("详情项编号")
    private String id;
    @ApiModelProperty("保留策略执行结果编号")
    private String executionId;
    @ApiModelProperty("详情项所指的镜像名")
    private String imageName;
    @ApiModelProperty("详情项状态")
    private String status;
    @ApiModelProperty("版本保留数量")
    private Integer retained;
    @ApiModelProperty("版本总数")
    private Integer total;
    @ApiModelProperty("执行结果详情项开始执行时间")
    private String startTime;
    @ApiModelProperty("执行结果详情项开始结束时间")
    private String endTime;
    @ApiModelProperty("执行结果详情项开始持续时间")
    private Long duration;
}
