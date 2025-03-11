package com.middleware.zeus.common.model.middleware.mongodb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2025/3/3 10:26 AM
 */
@Accessors(chain = true)
@Data
@ApiModel("企业版mongodb角色")
@AllArgsConstructor
@NoArgsConstructor
public class MongodbRoleDo {

    @ApiModelProperty("组织id")
    private String ordId;

    @ApiModelProperty("项目id")
    private String groupId;

    @ApiModelProperty("角色名称")
    private String roleName;

}
