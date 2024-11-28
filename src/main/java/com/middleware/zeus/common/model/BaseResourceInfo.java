package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2024/11/28 1:47 PM
 */
@Accessors(chain = true)
@NoArgsConstructor
@Data
@ApiModel("资源信息")
public class BaseResourceInfo {
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
}
