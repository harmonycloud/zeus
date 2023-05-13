package com.middleware.zeus.common.model.dashboard.redis;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2022/10/28 2:57 下午
 */
@ApiModel("redis list对象")
@Accessors(chain = true)
@Data
public class ListDto {

    @ApiModelProperty("序号(从0开始)")
    private String index;

    @ApiModelProperty("是否从头部执行操作")
    private boolean fromLeft;

    @ApiModelProperty("value")
    private String value;

    @ApiModelProperty("元素数量")
    private int count;

}
