package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2020/12/14
 */
@ApiModel(value = "制品服务仓库成员")
@Accessors(chain = true)
@Data
public class ProjectMember {

    @ApiModelProperty("用户id")
    private Integer userId;
    @ApiModelProperty("用户名称")
    private String userName;
    @ApiModelProperty("子系统用户类型")
    private String entityType;
    @ApiModelProperty("子系统成员自增id")
    private Integer id;
    @ApiModelProperty("制品服务仓库id")
    private String repositoryId;
    @ApiModelProperty("角色id")
    private Integer roleId;
    @ApiModelProperty("角色名称")
    private String roleName;

}
