package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author liyinlong
 * @since 2023/1/10 8:53 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("备份位置信息")
public class BackupPositionDTO {

    @ApiModelProperty("组织id")
    private String organId;

    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("备份位置id")
    private Integer id;

    @ApiModelProperty("备份服务器id")
    private Integer backupServerId;

    @ApiModelProperty("备份服务器详情id")
    private Integer backupServerDetailId;

    @ApiModelProperty("备份服务器名称")
    private String backupServerName;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("备份位置名称")
    private String name;

    @ApiModelProperty("备份位置")
    private String backupPosition;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("关联备份任务数")
    private Integer backupTaskNum;

}
