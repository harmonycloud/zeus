package com.middleware.zeus.common.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author LanChao
 * @date 2021-01-11 10:55:18
 */
@Accessors(chain = true)
@Data
@ApiModel("内部服务")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        property = "type",
        // defaultImpl = EdgeNginxLoadBalancer.class,
        visible = true)
public abstract class InternalService implements Serializable {
    @ApiModelProperty("InternalService类型")
    private String type;
    @ApiModelProperty("服务名称")
    private String name;
    @ApiModelProperty("集群id")
    private String clusterId;
    @ApiModelProperty("集群名称")
    private String clusterNickName;
    @ApiModelProperty("分区名称")
    private String namespace;
    @ApiModelProperty("分区显示名称")
    private String namespaceNickName;
    @ApiModelProperty("服务端口")
    private List<ServicePort> ports;
    @ApiModelProperty("服务ip")
    private String clusterIp;
    @ApiModelProperty("服务类型")
    private String serviceType;
    @ApiModelProperty("服务创建时间")
    private String createTime;
    @ApiModelProperty("服务关联workload的标签Selector")
    private Map<String, String> labelSelector;

    @ApiModelProperty("会话保持")
    private String sessionAffinity;
    @ApiModelProperty("按客户端IP最大会话保持时间")
    private Integer timeoutSeconds;

    /**
     * 关联部署
     */
    private String workloadType;
    private List<RelationWorkload> relationWorkloads;
    @ApiModelProperty("loadbalancer类型,增加annotation")
    private String lbType;
    @ApiModelProperty("loadbalancer类型时的ip")
    private List<String> vip;
}
