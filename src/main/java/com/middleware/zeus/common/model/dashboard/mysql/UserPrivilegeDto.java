package com.middleware.zeus.common.model.dashboard.mysql;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author liyinlong
 * @since 2022/12/9 2:08 下午
 */
@ApiModel("mysql用户权限")
@Accessors(chain = true)
@Data
public class UserPrivilegeDto {

    @ApiModelProperty("用户权限列表")
    private List<GrantOptionDto> grantOptionDtos;

}
