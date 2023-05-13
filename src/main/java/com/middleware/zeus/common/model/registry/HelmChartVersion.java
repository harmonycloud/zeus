package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author dengyulong
 * @date 2020/12/16
 */
@ApiModel("helm chart版本")
@Accessors(chain = true)
@Data
public class HelmChartVersion {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("版本号")
    private String version;

    @ApiModelProperty("api版本号")
    private String apiVersion;

    @ApiModelProperty("应用版本号")
    private String appVersion;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("摘要")
    private String digest;

    @ApiModelProperty("标签")
    private List<String> labels;

    @ApiModelProperty("url")
    private List<String> urls;

    @ApiModelProperty("创建时间")
    private String created;

}
