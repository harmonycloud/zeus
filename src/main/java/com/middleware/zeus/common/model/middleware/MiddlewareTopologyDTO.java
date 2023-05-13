package com.middleware.zeus.common.model.middleware;

import com.middleware.zeus.common.model.MonitorResourceQuota;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/2/24 8:24 下午
 */
@Data
@Accessors(chain = true)
@ApiModel("中间件拓扑图")
public class MiddlewareTopologyDTO {

    @ApiModelProperty("集群")
    private String clusterId;

    @ApiModelProperty("分区")
    private String namespace;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("类型")
    private String type;

    @ApiModelProperty("别名")
    private String aliasName;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("资源(源自prometheus)")
    private MonitorResourceQuota monitorResourceQuota;

    @ApiModelProperty("资源")
    private String storageClassName;

    @ApiModelProperty("存储引擎")
    private String provisioner;

    @ApiModelProperty("pod信息")
    private List<PodInfo> pods;

    @ApiModelProperty("中间件pod list组")
    private PodInfoGroup podInfoGroup;


}
