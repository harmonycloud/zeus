package com.middleware.zeus.common.model;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2020/12/9 4:42 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "集群")
public class Cluster {
    @ApiModelProperty("集群编号")
    private String id;
    @ApiModelProperty("集群所属数据中心编号")
    private String dcId;
    @ApiModelProperty("集群名称")
    private String name;
    @ApiModelProperty("集群别名/显示名称")
    private String nickname;
    @ApiModelProperty("集群类型，目前支持的类型有{'k8s'}")
    private String type;
    @ApiModelProperty("集群主版本号")
    private int majorVersion;
    @ApiModelProperty("集群次版本号")
    private int minorVersion;
    @ApiModelProperty("集群访问协议")
    private String protocol;
    @ApiModelProperty("集群访问域名/IP")
    private String host;
    @ApiModelProperty("集群访问端口")
    private int port;
    @ApiModelProperty("集群属性，不同集群属性不同")
    private JSONObject attributes;
    @ApiModelProperty("集群状态")
    private boolean normal;
}
