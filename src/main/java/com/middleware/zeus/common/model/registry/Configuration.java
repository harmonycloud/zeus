package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author dengyulong
 * @date 2020/12/03
 */
@Data
@ApiModel(description = "子系统配置")
public class Configuration {

    @ApiModelProperty("每个项目的镜像数量配额")
    private ConfigurationInfo countPerProject;

    @ApiModelProperty("每个项目的磁盘存储配额")
    private ConfigurationInfo storagePerProject;

}
