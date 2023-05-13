package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2020/12/14 1:40 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("镜像版本保留策略执行规则")
public class RetentionTiming {
    @ApiModelProperty("定时策略字符串(秒(0-59),分(0-59),小时(0-23),一个月的一天(1-31),月(1-12/JAN-DEC),一周的一天(0-6/SUN-SAT)")
    private String cron;
}
