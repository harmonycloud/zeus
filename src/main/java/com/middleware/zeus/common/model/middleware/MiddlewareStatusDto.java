package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/3/29 2:56 下午
 */
@Data
@ApiModel("中间件状态")
public class MiddlewareStatusDto {

    @ApiModelProperty("中间件类型：redis,mysql,elasticsearch,rocketmq")
    private String type;
    @ApiModelProperty("中间件")
    private List<Middleware> middlewareList;
    @ApiModelProperty("状态")
    private boolean status;
    @ApiModelProperty("总cpu")
    private Double totalCpu;
    @ApiModelProperty("总memory")
    private Double totalMemory;
    @ApiModelProperty("图片路径")
    private String imagePath;
}
