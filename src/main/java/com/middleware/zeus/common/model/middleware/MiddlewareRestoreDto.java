package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2023/5/6 4:00 下午
 */
@Accessors(chain = true)
@NoArgsConstructor
@Data
@ApiModel("中间件克隆信息")
public class MiddlewareRestoreDto {

    @ApiModelProperty("克隆服务所在集群id")
    private String clusterId;

    @ApiModelProperty("克隆服务所在分区")
    private String namespace;

    @ApiModelProperty("中间件类型")
    private String type;

    @ApiModelProperty("中间件名称")
    private String middlewareName;

    @ApiModelProperty("备份记录名称")
    private String backupName;

    @ApiModelProperty("恢复时间(非必填)")
    private String restoreTime;

    @ApiModelProperty("备份任务id")
    private String backupId;

    @ApiModelProperty("备份记录所在可用区域(非必填)")
    private String activeArea;

    @ApiModelProperty("备份源中间件名称")
    private String sourceName;

}
