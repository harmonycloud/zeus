package com.harmonycloud.zeus.integration.cluster.bean;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.ClusterCert;
import com.harmonycloud.caas.common.model.middleware.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@ApiModel
@Accessors(chain = true)
@Data
public class MiddlewareClusterInfo {

    @ApiModelProperty("集群证书信息")
    private ClusterCert cert;
    @ApiModelProperty("token")
    private String accessToken;
    @ApiModelProperty("协议")
    private String protocol;
    @ApiModelProperty("域名/IP")
    private String address;
    @ApiModelProperty("端口")
    private Integer port;
    @ApiModelProperty("对外服务")
    private List<MiddlewareClusterIngress> ingressList;
    @ApiModelProperty("存储相关信息")
    private Map<String, Object> storage;
    @ApiModelProperty("制品服务相关信息")
    private Registry registry;
    @ApiModelProperty("监控相关")
    private MiddlewareClusterMonitor monitor;
    @ApiModelProperty("集群属性")
    private JSONObject attributes;
    @ApiModelProperty("日志信息")
    private MiddlewareClusterLogging logging;
    @ApiModelProperty("资源对象")
    private ClusterQuotaDTO clusterQuotaDTO;

    public String getMasterUrl() {
        return this.protocol + "://" + this.address + (this.port == null ? "" : ":" + this.port);
    }

}
