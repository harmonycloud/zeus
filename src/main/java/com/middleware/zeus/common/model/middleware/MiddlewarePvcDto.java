package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author xutianhong
 * @Date 2023/1/10 11:05 上午
 */
@Accessors(chain = true)
@Data
@ApiModel("中间件pvc信息")
public class MiddlewarePvcDto {

    @ApiModelProperty("pvc名称")
    private String pvcName;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("访问策略")
    private String accessModes;

    @ApiModelProperty("存储大小")
    private Double storage;

    @ApiModelProperty("存储类型")
    private String storageClass;

    @ApiModelProperty("回收策略")
    private String reclaimPolicy;

    @ApiModelProperty("回收策略")
    private Date createTime;

}
