package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2020/12/15
 */
@Accessors(chain = true)
@Data
@ApiModel(value = "制品服务仓库的项目定额")
public class RegistryQuota {

    @ApiModelProperty("仓库id")
    private Integer id;

    @ApiModelProperty("仓库名")
    private String name;

    @ApiModelProperty("所属用户名")
    private String ownerName;

    @ApiModelProperty("配额")
    private ProjectSummaryQuota.ProjectQuota hard;

    @ApiModelProperty("使用量")
    private ProjectSummaryQuota.ProjectQuota used;

    @ApiModelProperty("创建时间")
    private String createTime;

    @ApiModelProperty("修改时间")
    private String updateTime;

}
