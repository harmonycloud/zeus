package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author damiao
 * @date 2020/12/16 4:59 PM
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "nginx负载均衡属性")
public class OpenYurtNginxLoadbalancerAttributes {
    @ApiModelProperty("所属边缘节点组名称")
    private String edgeNodePoolName;
    @ApiModelProperty("负载均衡端口")
    private Integer httpPort;
    @ApiModelProperty("入口所在主机")
    private List<String> nodeNames = new ArrayList<>();
    @ApiModelProperty("nginx配置")
    private Map<String, String> nginxConfig;
    // TODO 这些是不是用于其它功能的？比如组件健康检查，对外https服务等
    private int httpsPort;
    private int healthPort;
    private int statusPort;
    private Integer externalHttpPort;
    private Integer externalHttpsPort;
}
