package com.middleware.zeus.common.model.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @description 系统配置
 * @author  liyinlong
 * @since 2023/3/14 3:41 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("系统配置")
public class SystemConfigDto {

    @ApiModelProperty("configName")
    private String configName;

    @ApiModelProperty("configValue")
    private String configValue;

}
