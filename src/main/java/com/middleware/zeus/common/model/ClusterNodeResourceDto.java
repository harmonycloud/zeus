package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author xutianhong
 * @Date 2021/11/5 2:00 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "资源池主机资源列表")
public class ClusterNodeResourceDto {

    @ApiModelProperty("集群id")
    private String clusterId;
    @ApiModelProperty("id地址")
    private String ip;
    @ApiModelProperty("节点名称")
    private String nodeName;
    @ApiModelProperty("cpu使用量")
    private Double cpuUsed;
    @ApiModelProperty("cpu总量")
    private Double cpuTotal;
    @ApiModelProperty("cpu使用率")
    private Double cpuRate;
    @ApiModelProperty("memory使用量")
    private Double memoryUsed;
    @ApiModelProperty("memory总量")
    private Double memoryTotal;
    @ApiModelProperty("memory使用率")
    private Double memoryRate;
    @ApiModelProperty("状态")
    private String status;
    @ApiModelProperty("创建时间")
    private Date createTime;
    @ApiModelProperty("是否可调度")
    private Boolean scheduable;
    @ApiModelProperty("节点角色")
    private String role;

}
