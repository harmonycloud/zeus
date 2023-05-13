package com.middleware.zeus.common.model.dashboard.redis;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2022/10/27 9:39 上午
 */
@ApiModel("redis zset对象")
@Accessors(chain = true)
@Data
public class ZSetDto {

    @ApiModelProperty("权重值")
    private String score;

    @ApiModelProperty("内容")
    private String member;

}
