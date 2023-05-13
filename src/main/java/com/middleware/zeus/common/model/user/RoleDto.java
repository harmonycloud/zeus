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
 * @Date 2021/9/8 3:23 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("角色")
public class RoleDto {

    @ApiModelProperty("id")
    private Integer id;
    @ApiModelProperty("角色名称")
    private String name;
    @ApiModelProperty("角色类型: manager/normal")
    private String type;
    @ApiModelProperty("角色描述")
    private String description;
    @ApiModelProperty("权重")
    private Integer weight;
    @ApiModelProperty("创建时间")
    private Date createTime;
    @ApiModelProperty("角色中间件权限")
    private Map<String, String> power;
    @ApiModelProperty("角色菜单权限")
    private List<ResourceMenuDto> resourceMenuDtoList;

}
