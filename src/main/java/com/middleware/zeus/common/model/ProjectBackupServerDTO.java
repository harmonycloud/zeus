package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2023/1/12 3:10 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("项目备份服务器关联信息")
public class ProjectBackupServerDTO {

    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("备份服务器id")
    private Integer backupServerId;

    @ApiModelProperty("备份服务器名称")
    private String backupServerName;

}
