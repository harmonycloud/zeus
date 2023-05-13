package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2021/1/8 10:02 上午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "制品库垃圾清理策略")
public class RegistryGCPolicy {
    @ApiModelProperty("策略类型，" +
            "包括'Hourly(每时)', 'Daily(每天)', 'Weekly(每周)', 'Custom(自定义)', 'None(无)', 'Manual(手动执行)'")
    private String type;
    @ApiModelProperty("定时任务")
    private String cron;
}
