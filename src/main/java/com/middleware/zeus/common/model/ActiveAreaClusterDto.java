package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2022/8/13 1:46 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "可用区集群对象")
public class ActiveAreaClusterDto {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("集群中文名")
    private String clusterAliasName;

    @ApiModelProperty("是否开启可用区")
    private Boolean activeActive;

    @ApiModelProperty("可用区数量")
    private Integer activeAreaNum;
    /**
     * 集群状态 1:正常，0：异常
     */
    @ApiModelProperty("集群状态")
    private Integer statusCode;

}
