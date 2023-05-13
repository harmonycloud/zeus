package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2020/12/15 9:11 上午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "镜像版本保留策略执行结果")
public class RetentionExecution {
    @ApiModelProperty("执行结果编号")
    private String id;
    @ApiModelProperty("执行结果状态")
    private String status;
    @ApiModelProperty("是否模型运行(true-模拟运行)")
    private Boolean dryRun;
    @ApiModelProperty("执行过程触发原因(执行类型)")
    private String trigger;
    @ApiModelProperty("执行开始时间")
    private String startTime;
    @ApiModelProperty("执行结束时间")
    private String endTime;
    @ApiModelProperty("执行持续时间")
    private Long duration;
}