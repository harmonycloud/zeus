package com.middleware.zeus.common.model.user;

import com.middleware.zeus.common.model.ResourceQuotaDo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/7 11:02 上午
 */
@Accessors(chain = true)
@Data
@ApiModel("组织")
public class OrganizationDto {

    @ApiModelProperty("组织id")
    private String organId;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("项目数")
    private Integer projectCount;

    @ApiModelProperty("用户数")
    private Integer userCount;

    @ApiModelProperty("组织管理员用户名")
    private String organizationManager;

    @ApiModelProperty("组织管理员角色id")
    private Integer organizationManagerRoleId;

    @ApiModelProperty("用户列表")
    private List<UserDto> userDtoList;


}
