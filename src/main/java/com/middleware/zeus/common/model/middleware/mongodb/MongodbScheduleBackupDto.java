package com.middleware.zeus.common.model.middleware.mongodb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2025/7/31 10:51
 */
@Data
@Accessors(chain = true)
@ApiModel("mongodb备份")
public class MongodbScheduleBackupDto {

    @ApiModelProperty("集群")
    private String clusterId;

    @ApiModelProperty("命名空间")
    private String namespace;

    @ApiModelProperty("中间件名称")
    private String name;

    @ApiModelProperty("快照间隔小时：6 12 18 24")
    private Integer snapshotIntervalHours;

    @ApiModelProperty("最近快照的保留天数: 2 3 4 5")
    private Integer snapshotRetentionDays;

    @ApiModelProperty("每日快照保留时间")
    private Integer dailySnapshotRetentionDays;

    @ApiModelProperty("每周快照保留时间")
    private Integer weeklySnapshotRetentionWeeks;

    @ApiModelProperty("每月快照保留时间")
    private Integer monthlySnapshotRetentionMonths;

    @ApiModelProperty("支持创建时间点快照的历史时间范围")
    private Integer pointInTimeWindowHours;

    @ApiModelProperty("执行全量备份的时间")
    private String fullIncrementalDayOfWeek;

}
