package com.harmonycloud.zeus.integration.registry.bean.harbor;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
@ApiModel("helm list命令返回的信息")
@Accessors(chain = true)
@Data
public class HelmListInfo {

    @ApiModelProperty("名称")
    private String name;
    @ApiModelProperty("app版本")
    private String namespace;
    @ApiModelProperty("当前release版本")
    private String revision;
    @ApiModelProperty("更新时间")
    private String updateTime;
    @ApiModelProperty("状态：failed，deployed")
    private String status;
    @ApiModelProperty("chart包版本")
    private String chart;
    @ApiModelProperty("app版本")
    private String appVersion;

}
