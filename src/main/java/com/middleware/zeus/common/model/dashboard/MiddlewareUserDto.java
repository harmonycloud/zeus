package com.middleware.zeus.common.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/10/10 3:51 下午
 */
@ApiModel("中间件用户对象")
@Accessors(chain = true)
@Data
public class MiddlewareUserDto {

    @ApiModelProperty("id")
    private String id;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("是否启用")
    private boolean usable;

    @ApiModelProperty("是否继承权限")
    private boolean inherit;

    @ApiModelProperty("用户权限")
    private List<MiddlewareUserAuthority> authorityList;

    @ApiModelProperty("上次登录时间")
    private Date lastLoginTime;

}
