package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author xutianhong
 * @Date 2021/3/31 2:31 下午
 */
@Data
@ApiModel("中间件资源实时使用量")
public class MiddlewareMonitorDto {

    @ApiModelProperty("时间")
    private String time;
    @ApiModelProperty("cpu使用量")
    private Double cpu;
    @ApiModelProperty("cpu总量")
    private Double cpuTotal;
    @ApiModelProperty("cpu使用率")
    private Double cpuRate;
    @ApiModelProperty("memory使用量")
    private Double memory;
    @ApiModelProperty("memory总量")
    private Double memoryTotal;
    @ApiModelProperty("memory使用率")
    private Double memoryRate;
    @ApiModelProperty("卷使用量")
    private Double storage;
    @ApiModelProperty("卷总量")
    private Double storageTotal;
    @ApiModelProperty("卷使用率")
    private Double storageRate;

}
