package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/11/3 11:27 上午
 */
@Accessors(chain = true)
@NoArgsConstructor
@Data
@ApiModel("中间件资源信息")
public class MiddlewareResourceInfo {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区")
    private String namespace;

    @ApiModelProperty("中间件名称")
    private String name;

    @ApiModelProperty("中间件中文别名")
    private String aliasName;

    @ApiModelProperty("中间件类型")
    private String type;

    @ApiModelProperty("中间件版本")
    private String chartVersion;

    @ApiModelProperty("cpu配额")
    private Double requestCpu;

    @ApiModelProperty("memory配额")
    private Double requestMemory;

    @ApiModelProperty("storage配额")
    private Double requestStorage;

    @ApiModelProperty("5分钟cpu平均使用量")
    private Double per5MinCpu;

    @ApiModelProperty("5分钟memory平均使用量")
    private Double per5MinMemory;

    @ApiModelProperty("5分钟storaeg平均使用量")
    private Double per5MinStorage;

    @ApiModelProperty("每分钟cpu平均使用量")
    private Double cpuRate;

    @ApiModelProperty("每分钟cpu平均使用量")
    private Double memoryRate;

    @ApiModelProperty("每分钟memory平均使用量")
    private Double storageRate;

    @ApiModelProperty("图片地址")
    private String imagePath;

    public MiddlewareResourceInfo(String clusterId, String namespace, String name, String type) {
        this.clusterId = clusterId;
        this.namespace = namespace;
        this.name = name;
        this.type = type;
    }

}
