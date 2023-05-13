package com.middleware.zeus.common.model.user;

import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/3/24 9:26 上午
 */
@Accessors(chain = true)
@Data
@ApiModel("项目")
public class ProjectDto {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("别名")
    private String aliasName;

    @ApiModelProperty("备注")
    private String description;

    @ApiModelProperty("项目管理员")
    private String user;

    @ApiModelProperty("集群编号")
    private List<MiddlewareClusterDTO> clusterList;

    @ApiModelProperty("组织id")
    private String organId;

    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("成员数")
    private Integer memberCount;

    @ApiModelProperty("命名空间数")
    private Integer namespaceCount;

    @ApiModelProperty("中间件数量")
    private Integer middlewareCount;

    @ApiModelProperty("用户项目中对应角色id")
    private Integer roleId;

    @ApiModelProperty("用户项目中对应角色名称")
    private String roleName;

    @ApiModelProperty("用户项目中对应角色")
    private Integer roleWeight;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("用户列表")
    private List<UserDto> userDtoList;

    @ApiModelProperty("租户名称")
    private String tenantAliasName;

    @ApiModelProperty("项目可用备份服务器列表")
    private List<Integer> backupServerList;

    @ApiModelProperty("项目管理员名称列表(观云台对接使用)")
    private String pmUserList;

}