package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2023/5/8 11:05 上午
 */
@Data
@Accessors(chain = true)
@ApiModel("告警用户")
public class AlertUserDo {

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("用户名")
    private String alisaName;

    @ApiModelProperty("告警类型")
    private String alertType;

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区")
    private String namespace;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("邮箱告警")
    private Boolean mailAlert;

    @ApiModelProperty("短信告警")
    private Boolean messageAlert;

    @ApiModelProperty("邮箱地址")
    private String email;

    @ApiModelProperty("手机号")
    private String phone;
}
