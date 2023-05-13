package com.middleware.zeus.common.model.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2023/1/11 2:19 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("角色")
public class ProjectNamespaceDo {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区名称")
    private String namespace;

    @ApiModelProperty("组织id")
    private String organId;

    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

}
