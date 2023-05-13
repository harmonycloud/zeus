package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2021/1/4 4:49 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("租户")
public class Tenant {
    @ApiModelProperty("租户编号")
    private String id;
    @ApiModelProperty("租户名")
    private String name;
    @ApiModelProperty("租户显示名称")
    private String nickname;
}
