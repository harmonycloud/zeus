package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2021/1/8 10:07 上午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "制品库垃圾清理任务")
public class RegistryGCTask {
    @ApiModelProperty("任务id")
    private String taskId;
    @ApiModelProperty("任务触发类型，" +
            "包括'Hourly(每时)', 'Daily(每天)', 'Weekly(每周)', 'Custom(自定义)', 'None(无)', 'Manual(手动执行)'")
    private String type;
    @ApiModelProperty("任务状态：" +
            "0-pending 未开始, " +
            "1-running 正在运行, " +
            "2-error 发生错误, " +
            "3-stopped 已停止, " +
            "4-finished 已完成, " +
            "5-canceled 已取消, " +
            "6-retrying 正在重试, " +
            "7-_continue 正在切换状态, " +
            "8-scheduled 已调度)")
    private Integer status;
    @ApiModelProperty("创建时间")
    private String createTime;
    @ApiModelProperty("更新时间")
    private String updateTime;
}
