package com.middleware.zeus.common.model.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2023/3/13 5:50 下午
 */
@Accessors(chain = true)
@ApiModel("项目配额")
@Data
public class ProjectQuota extends OrganizationQuota {

    @ApiModelProperty("项目id")
    private String projectId;
}
