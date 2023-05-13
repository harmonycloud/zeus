package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author xutianhong
 * @since 2021/6/23 10:55 上午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "secret")
public class Secret {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("命名空间")
    private String namespace;

    @ApiModelProperty("secret名称")
    private String name;

    @ApiModelProperty("数据")
    private Map<String, String> data;

    @ApiModelProperty("标签")
    private Map<String, String> labels;

}
