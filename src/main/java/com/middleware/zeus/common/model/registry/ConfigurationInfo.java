package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author dengyulong
 * @date 2020/12/03
 */
@Data
@ApiModel(description = "子系统配置信息")
public class ConfigurationInfo {

    @ApiModelProperty("是否可修改")
    private boolean editable;

    @ApiModelProperty("配置值")
    private String value;

}
