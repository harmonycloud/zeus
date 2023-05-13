package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2020/12/10
 */
@ApiModel(value = "制品服务用户")
@Accessors(chain = true)
@Data
public class RegistryUser {

    /**
     * 这5个为创建用户时使用
     */
    @ApiModelProperty("用户名")
    private String username;
    @ApiModelProperty("用户真实姓名")
    private String realname;
    @ApiModelProperty("密码")
    private String password;
    @ApiModelProperty("邮箱")
    private String email;
    @ApiModelProperty("备注")
    private String comment;

    /**
     * 查询列表后会多返回下面数据
     */
    @ApiModelProperty("用户id")
    private String userId;
    @ApiModelProperty("角色id")
    private Integer roleId;
    @ApiModelProperty("是否为管理员")
    private Boolean hasAdminRole;
    @ApiModelProperty("是否已删除")
    private String deleted;
    @ApiModelProperty("创建时间")
    private String createTime;

}
