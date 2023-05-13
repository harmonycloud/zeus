package com.middleware.zeus.common.model.dashboard.redis;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2022/10/27 9:41 上午
 */
@ApiModel("redis hash对象")
@Accessors(chain = true)
@Data
public class HashDto {

    @ApiModelProperty("hash 键")
    private String field;

    @ApiModelProperty("hash 值")
    private String value;

}
