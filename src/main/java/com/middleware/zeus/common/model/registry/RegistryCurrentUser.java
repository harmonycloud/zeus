package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2020/11/25
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "镜像子系统当前用户信息")
public class RegistryCurrentUser {

    @ApiModelProperty("用户id")
    private Integer userId;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("用户真实姓名")
    private String realName;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("用户密码")
    private String password;

    @ApiModelProperty("用户密码加密方式")
    private String passwordVersion;

    @ApiModelProperty("备注")
    private String comment;

    @ApiModelProperty("角色id")
    private Integer roleId;

    @ApiModelProperty("角色名称")
    private String roleName;

    @ApiModelProperty("是否系统管理员")
    private boolean sysAdmin;

    @ApiModelProperty("是否已删除")
    private boolean deleted;

    @ApiModelProperty("创建时间")
    private String createTime;

    @ApiModelProperty("修改时间")
    private String updateTime;

}
