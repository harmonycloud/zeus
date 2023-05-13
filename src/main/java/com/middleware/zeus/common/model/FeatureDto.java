package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2023/5/4 7:22 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("特性功能")
public class FeatureDto {

    @ApiModelProperty("功能名称")
    private String name;

    @ApiModelProperty("是否开启")
    private Boolean enabled;

}
