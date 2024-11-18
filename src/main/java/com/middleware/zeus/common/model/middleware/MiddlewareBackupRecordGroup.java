package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/10 7:48 下午
 */
@ApiModel("备份记录组")
@Accessors(chain = true)
@Data
public class MiddlewareBackupRecordGroup {

    @ApiModelProperty("备份任务名称")
    private String taskName;

    @ApiModelProperty("备份任务id")
    private String backupId;

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("命名空间")
    private String namespace;

    @ApiModelProperty("中间件名称")
    private String sourceName;

    @ApiModelProperty("中间件类型")
    private String sourceType;

    @ApiModelProperty("中间件状态(0：已删除，1：未删除)")
    private Integer sourceStatus;

    @ApiModelProperty("备份状态")
    private String phrase;

    @ApiModelProperty("是否停止")
    private String pause;

    @ApiModelProperty("备份任务类型（1：普通，2：双活）")
    private Integer taskType;

    @ApiModelProperty("备份方式(period:周期，single：单次)")
    private String backupMode;

    @ApiModelProperty("中间件类型图标")
    private String imagePath;

    @ApiModelProperty("备份地址")
    private List<String> backupAddresses;

    @ApiModelProperty("备份记录列表")
    private List<MiddlewareBackupRecord> middlewareBackupRecords;

}
