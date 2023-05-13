package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2020/12/16
 */
@ApiModel("helm chart")
@Accessors(chain = true)
@Data
public class HelmChart {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("总共版本数")
    private Integer totalVersions;

    @ApiModelProperty("最新版本")
    private String latestVersion;

    @ApiModelProperty("是否过期")
    private Boolean deprecated;

    private String home;

    @ApiModelProperty("图标")
    private String icon;

    @ApiModelProperty("创建时间")
    private String created;

    @ApiModelProperty("修改时间")
    private String updated;

}
