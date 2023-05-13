package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author xutianhong
 * @Date 2022/10/28 10:30 上午
 */
@Data
@ApiModel("ldap服务器信息")
public class LicenseInfoDto {

    @ApiModelProperty("授权用户")
    private String user;

    @ApiModelProperty("授权类型")
    private String type;

    @ApiModelProperty("识别码")
    private String code;

    @ApiModelProperty("生产环境")
    private MonitorResourceQuotaBase produce;

    @ApiModelProperty("测试环境")
    private MonitorResourceQuotaBase test;
}
