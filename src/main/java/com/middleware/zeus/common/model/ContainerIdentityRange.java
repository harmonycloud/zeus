package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2023/4/24 10:47 上午
 */
@ApiModel("id范围")
@Accessors(chain = true)
@Data
public class ContainerIdentityRange {

    @ApiModelProperty("id最小值")
    private Long min;

    @ApiModelProperty("id最大值")
    private Long max;

}
