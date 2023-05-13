package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2021/1/4 4:50 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("项目")
public class Project {
    @ApiModelProperty("项目编号")
    private String id;
    @ApiModelProperty("项目名")
    private String name;
    @ApiModelProperty("项目显示名称")
    private String nickname;
    @ApiModelProperty("项目所属租户")
    private Tenant tenant;
}
