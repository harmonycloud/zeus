package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/12/16 4:29 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("中间件values")
public class MiddlewareValues {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区")
    private String namespace;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("类型")
    private String type;

    @ApiModelProperty("版本")
    private String chartVersion;

    @ApiModelProperty("values.yaml")
    private String values;



}
