package com.middleware.zeus.common.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author lanchao
 * @date 2020/12/16 4:59 PM
 */
@Accessors(chain = true)
@Data
@ApiModel("对外路由")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        property = "type",
        // defaultImpl = EdgeNginxLoadBalancer.class,
        visible = true)
public abstract class Ingress {
    @ApiModelProperty("名称")
    private String name;
    @ApiModelProperty("对外路由类型")
    private String type;
    @ApiModelProperty("所属集群id")
    private String clusterId;
    @ApiModelProperty("所属集群显示名称")
    private String clusterNickname;
    @ApiModelProperty("命名空间")
    private String namespace;
    @ApiModelProperty("命名空间显示名称")
    private String namespaceNickname;
    @ApiModelProperty("负载均衡名称")
    private String loadbalancerName;
    @ApiModelProperty("负载均衡显示名称")
    private String loadbalancerNickName;
    @ApiModelProperty("负载均衡类型")
    private String loadbalancerType;
    @ApiModelProperty("服务类型(协议)")
    private String protocol;
    @ApiModelProperty("是否验证证书")
    private Integer passthrough;
    @ApiModelProperty("证书")
    private String certificate;
    @ApiModelProperty("域名")
    private String domain;
    @ApiModelProperty("对外暴露服务列表")
    private List<IngressService> rules;


    @ApiModelProperty("是否会话保持")
    private Boolean stickySessionsFlag;
    //最大会话保持时间 默认10800
    @ApiModelProperty("最大保持时间")
    private Integer timeoutSeconds;
    //默认INGRESSCOOKIE
    @ApiModelProperty("cookie名称")
    private String cookieName;
    @ApiModelProperty("cookie路径")
    private String cookiePath;

}
