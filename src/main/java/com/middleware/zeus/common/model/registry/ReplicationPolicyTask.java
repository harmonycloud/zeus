package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2020/12/20 8:47 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("镜像复制策略执行任务条目")
public class ReplicationPolicyTask {
    @ApiModelProperty("任务编号")
    private String id;
    @ApiModelProperty("任务名")
    private String name;
    @ApiModelProperty("任务状态")
    private String status;
    @ApiModelProperty("任务操作")
    private String op;
    @ApiModelProperty("开始时间")
    private String startTime;
    @ApiModelProperty("结束时间")
    private String endTime;
}
