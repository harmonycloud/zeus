package com.middleware.zeus.common.model.middleware;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/23 4:52 下午
 */
@Data
@Accessors(chain = true)
@ApiModel("中间件自定义配置")
public class MiddlewareCustomConfig {

    @ApiModelProperty("集群")
    private String clusterId;

    @ApiModelProperty("分区")
    private String namespace;

    @ApiModelProperty("中间件名称")
    private String name;

    @ApiModelProperty("中间件类型")
    private String type;

    @ApiModelProperty("是否重启服务")
    private Boolean reboot;

    @ApiModelProperty("节点类型")
    private String role;

    @ApiModelProperty("配置内容")
    private List<CustomConfig> customConfigList;
}
