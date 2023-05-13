package com.middleware.zeus.common.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author damiao
 * @date 2020/12/16 4:59 PM
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "负载均衡")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        property = "type",
        // defaultImpl = EdgeNginxLoadBalancer.class,
        visible = true)
public abstract class Loadbalancer {
    @ApiModelProperty("名称")
    private String name;
    @ApiModelProperty("显示名称")
    private String nickname;
    @ApiModelProperty("负载均衡类型")
    private String type;
    @ApiModelProperty("分区")
    private String namespace;
    @ApiModelProperty("创建时间")
    private String createTime;
    @ApiModelProperty("域名")
    private List<String> domains;
    @ApiModelProperty("访问地址(ip)")
    private List<String> accessIPs;
    @ApiModelProperty("所分配项目")
    private List<Project> projects;
    @ApiModelProperty("是否为默认")
    private Boolean isDefault;
    @ApiModelProperty("负载均衡状态")
    private String status;
}
