package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2024/11/7 9:22 AM
 */
@Accessors(chain = true)
@Data
@ApiModel("备份恢复时间")
public class BackupRestoreTimeDto {

    @ApiModelProperty("备份id")
    private String backupId;

    @ApiModelProperty("组织id")
    private String organId;

    @ApiModelProperty("起始时间")
    private Date startTime;

    @ApiModelProperty("(最新一次备份)结束时间")
    private Date endTime;

    @ApiModelProperty("时间范围")
    private List<TimeRange> timeRange;


    // 内部类 TimeRange
    @Accessors(chain = true)
    @Data
    @ApiModel("时间范围")
    public static class TimeRange {

        @ApiModelProperty("开始时间")
        private String start;

        @ApiModelProperty("结束时间")
        private String end;

    }

}
