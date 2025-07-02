package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("ldap服务器信息")
public class LdapConfigDto implements Serializable {


    @ApiModelProperty("服务器地址")
    private String url;

    @ApiModelProperty("基准DN")
    private String base;

    @ApiModelProperty("管理DN")
    private String username;

    @ApiModelProperty("管理DN密码")
    private String password;

    @ApiModelProperty("对象类型")
    private String objectType;

    @ApiModelProperty("账户属性")
    private String accountType;

    @ApiModelProperty("用户名映射")
    private String displayName;

    @ApiModelProperty("邮箱映射")
    private String mail;

    @ApiModelProperty("手机号映射")
    private String phone;

    @ApiModelProperty("过滤条件")
    private String filterCondition;

}