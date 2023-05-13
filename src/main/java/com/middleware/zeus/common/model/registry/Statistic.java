package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2020/12/02
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "制品服务的统计信息")
public class Statistic {

    @ApiModelProperty("私有项目数")
    private Integer privateProjectCount;

    @ApiModelProperty("私有镜像数")
    private Integer privateRepoCount;

    @ApiModelProperty("公共项目数")
    private Integer publicProjectCount;

    @ApiModelProperty("公共项目数")
    private Integer publicRepoCount;

    @ApiModelProperty("项目总数")
    private Integer totalProjectCount;

    @ApiModelProperty("镜像总数")
    private Integer totalRepoCount;

    @ApiModelProperty("chart总数")
    private Long totalChartCount;

}