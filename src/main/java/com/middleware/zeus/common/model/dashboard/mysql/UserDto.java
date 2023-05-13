package com.middleware.zeus.common.model.dashboard.mysql;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @description 用户对象
 * @author  liyinlong
 * @since 2022/10/19 6:04 下午
 */
@ApiModel("mysql用户对象")
@Accessors(chain = true)
@Data
public class UserDto {

    @ApiModelProperty("id")
    private String id;

    @ApiModelProperty("用户名")
    private String user;

    @ApiModelProperty("允许用户登陆MySQL所使用的IP地址")
    private String host;

    @ApiModelProperty("新用户名")
    private String newUser;

    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("是否可授权")
    private boolean grantAble;

    @ApiModelProperty("是否启用")
    private boolean usable;

}
