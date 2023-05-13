package com.middleware.zeus.common.model.middleware;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.model.ClusterCert;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@ApiModel("中间件集群信息")
@Accessors(chain = true)
@Data
public class MiddlewareClusterDTO implements Serializable {

    private static final long serialVersionUID = -5975504736603262824L;

    @ApiModelProperty("集群编号")
    private String id;
    @ApiModelProperty("集群类型")
    private String type;
    @ApiModelProperty("集群所属数据中心编号")
    private String dcId;
    @ApiModelProperty("集群名称")
    private String name;
    @ApiModelProperty("集群别名/显示名称")
    private String nickname;
    @ApiModelProperty("annotations")
    private Map<String, String> annotations;
    @ApiModelProperty("token")
    private String accessToken;
    @ApiModelProperty("协议")
    private String protocol;
    @ApiModelProperty("域名/IP")
    private String host;
    @ApiModelProperty("端口")
    private Integer port;
    @ApiModelProperty("ingress信息")
    private List<MiddlewareClusterIngress> ingressList;
    @ApiModelProperty("存储相关信息")
    private MiddlewareClusterStorage storage;
    @ApiModelProperty("制品服务相关信息")
    private Registry registry;
    @ApiModelProperty("集群属性")
    private JSONObject attributes;
    @ApiModelProperty("证书信息")
    private ClusterCert cert;
    @ApiModelProperty("日志信息")
    private MiddlewareClusterLogging logging;
    @ApiModelProperty("资源对象")
    private ClusterQuotaDTO clusterQuotaDTO;
    @ApiModelProperty("是否可移除")
    private Boolean removable;
    @ApiModelProperty("组件安装")
    private ComponentsInstall componentsInstall;
    @ApiModelProperty("分区列表")
    private List<Namespace> namespaceList;
    @ApiModelProperty("是否双活")
    private Boolean activeActive;
    @ApiModelProperty("集群状态")
    private Integer statusCode;
    @ApiModelProperty("集群状态")
    private Integer activeAreaNum;

    public String getAddress() {
        return this.protocol + "://" + this.host + (this.port == null ? "" : ":" + this.port);
    }

}
