package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Accessors(chain = true)
@Data
@ApiModel("中间件配额")
public class MiddlewareQuota {

    @ApiModelProperty("cpu，可以不传单位，默认Core")
    private String cpu;

    @ApiModelProperty("内存，可以不传单位，默认Gi")
    private String memory;

    @ApiModelProperty("最大cpu，可以不传单位，默认Core")
    private String limitCpu;

    @ApiModelProperty("最大内存，可以不传单位，默认Gi")
    private String limitMemory;

    @ApiModelProperty("存储服务名称")
    private String storageClassName;

    @ApiModelProperty("存储服务别名")
    private String storageClassAliasName;

    @ApiModelProperty("存储服务配额，后台会自动拼Gi")
    private String storageClassQuota;

    @ApiModelProperty("存储引擎")
    private String provisioner;

    @ApiModelProperty("数量")
    private Integer num;

    // 以下字段仅在返回时使用
    /**
     * 是否是lvm类型的存储，
     */
    private Boolean isLvmStorage;

    /**
     * 存储配额值
     */
    private float storageClassQuotaValue;

}
