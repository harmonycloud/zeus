package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author xuutianhong
 * @date 2023/5/8 下午2:05
 */
@Accessors(chain = true)
@Data
@ApiModel("告警用户")
public class AlertUserDto {

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("别名")
    private String aliasName;

    @ApiModelProperty("邮箱")
    private String mail;

    @ApiModelProperty("手机")
    private String phone;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("邮箱告警")
    private Boolean mailAlert;

    @ApiModelProperty("短信告警")
    private Boolean messageAlert;
}
