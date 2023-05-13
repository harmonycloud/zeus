package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("ldap服务器信息")
public class LdapConfigDto implements Serializable {
    private Integer id;

    @ApiModelProperty("ip地址")
    private String ip;

    @ApiModelProperty("端口")
    private String port;

    @ApiModelProperty("基准DN")
    private String base;

    @ApiModelProperty("管理DN")
    private String userdn;

    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("是否开启ldap,1:开启，0:关闭")
    private Integer isOn;

    @ApiModelProperty("过滤条件")
    private String objectClass;

    @ApiModelProperty("用户属性名")
    private String searchAttribute;

    @ApiModelProperty("用户姓名属性名")
    private String displayNameAttribute;

}