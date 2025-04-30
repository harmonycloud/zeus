package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2025/4/29 上午10:44
 */
@ApiModel("中间件总览详细信息")
@Accessors(chain = true)
@Data
public class MiddlewareOverviewInfoDto {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("命名空间")
    private String namespace;

    @ApiModelProperty("组织id")
    private String organId;

    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("中间件名称")
    private String name;

    @ApiModelProperty("中间件状态")
    private String status;

}
