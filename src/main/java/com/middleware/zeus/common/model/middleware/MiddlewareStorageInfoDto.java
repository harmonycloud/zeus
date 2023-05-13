package com.middleware.zeus.common.model.middleware;

import com.middleware.zeus.common.model.MonitorResourceQuota;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/6/17 9:35 上午
 */
@Data
@ApiModel("中间件存储信息")
public class MiddlewareStorageInfoDto {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("集群中文名")
    private String clusterAliasName;

    @ApiModelProperty("分区")
    private String namespace;

    @ApiModelProperty("分区别名")
    private String namespaceAliasName;

    @ApiModelProperty("项目ID")
    private String projectId;

    @ApiModelProperty("项目中文名")
    private String projectAliasName;

    @ApiModelProperty("中间件名称")
    private String middlewareName;

    @ApiModelProperty("中间件别名")
    private String middlewareAliasName;

    @ApiModelProperty("中间件类型")
    private String type;

    @ApiModelProperty("中间件状态")
    private String status;

    @ApiModelProperty("pod信息")
    private List<PodInfo> pods;

    @ApiModelProperty("pod信息")
    private Integer podNum;

    @ApiModelProperty("监控信息")
    private MonitorResourceQuota monitorResourceQuota;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("图标地址")
    private String imagePath;

}
