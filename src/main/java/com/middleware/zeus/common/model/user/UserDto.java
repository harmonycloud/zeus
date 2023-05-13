package com.middleware.zeus.common.model.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/7/22 12:01 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("用户")
public class UserDto {

    @ApiModelProperty("用户ID")
    private Integer id;

    @ApiModelProperty("用户账号")
    private String userName;

    @ApiModelProperty("用户名")
    private String aliasName;

    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("手机")
    private String phone;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("密码创建时间")
    private Date passwordTime;

    @ApiModelProperty("角色列表")
    List<UserRole> userRoleList;

    @ApiModelProperty("关联角色id")
    private Integer roleId;

    @ApiModelProperty("关联角色")
    private String roleName;

    @ApiModelProperty("角色权限")
    private Map<String, String> power;

    @ApiModelProperty("是否为admin")
    private Boolean isAdmin;

    @ApiModelProperty("管理类型角色id")
    private Integer manager;

}
