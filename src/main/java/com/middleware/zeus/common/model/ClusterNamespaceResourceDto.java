package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/11/8 9:19 上午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "资源池命名空间资源列表")
public class ClusterNamespaceResourceDto {


    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区名称")
    private String name;

    @ApiModelProperty("cpu配额")
    private Double cpuRequest;


    @ApiModelProperty("5分钟cpu平均使用量")
    private Double per5MinCpu;


    @ApiModelProperty("cpu使用率")
    private Double cpuRate;

    @ApiModelProperty("memory配额")
    private Double memoryRequest;


    @ApiModelProperty("5分钟memory平均使用量")
    private Double per5MinMemory;


    @ApiModelProperty("memory使用率")
    private Double memoryRate;

    @ApiModelProperty("pvc配额")
    private Double pvcRequest;


    @ApiModelProperty("5分钟pvc平均使用量")
    private Double per5MinPvc;


    @ApiModelProperty("pvc使用率")
    private Double pvcRate;

}
