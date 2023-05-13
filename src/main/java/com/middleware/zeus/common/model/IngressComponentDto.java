package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/11/22 5:43 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "集群ingress组件")
public class IngressComponentDto {

    @ApiModelProperty("id")
    private Integer id;
    @ApiModelProperty("集群id")
    private String clusterId;
    /**
     * ingress类型：nginx或traefik
     */
    @ApiModelProperty("ingress类型")
    private String type;
    @ApiModelProperty("ingressName")
    private String name;
    @ApiModelProperty("ingress名称")
    private String ingressClassName;
    @ApiModelProperty("地址")
    private String address;
    @ApiModelProperty("configmap名称")
    private String configMapName;
    @ApiModelProperty("configmap所在分区")
    private String namespace;
    @ApiModelProperty("http端口")
    private String httpPort;
    @ApiModelProperty("https端口")
    private String httpsPort;
    @ApiModelProperty("healthz端口")
    private String healthzPort;
    @ApiModelProperty("defaultServer端口")
    private String defaultServerPort;
    @ApiModelProperty("监控端口(traefik专有)")
    private String monitorPort;
    @ApiModelProperty("dashboard端口(traefik专有)")
    private String dashboardPort;
    @ApiModelProperty("traefik端口组")
    private List<TraefikPort> traefikPortList;
    @ApiModelProperty("状态")
    private Integer status;
    @ApiModelProperty("创建时间")
    private Date createTime;
    @ApiModelProperty("创建后经过时间")
    private long seconds;
    @ApiModelProperty("节点亲和")
    private List<AffinityDTO> nodeAffinity;
    @ApiModelProperty("污点容忍")
    private List<String> tolerations;
    @ApiModelProperty("跳过端口冲突")
    private Boolean skipPortConflict;

}
