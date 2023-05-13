package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2021/1/21 9:46 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("构建历史(dockerfile)")
public class ImageBuildHistory {
    @ApiModelProperty("构建时间")
    private String buildTime;
    @ApiModelProperty("构建命令")
    private String command;
}
