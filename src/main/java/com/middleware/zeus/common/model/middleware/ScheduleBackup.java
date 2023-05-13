package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/7 1:50 下午
 */
@Data
@Accessors(chain = true)
public class ScheduleBackup {

    @ApiModelProperty("名称")
    private String name;
    @ApiModelProperty("分区")
    private String namespace;
    @ApiModelProperty("备份时间（最近一次）")
    private String lastBackupTime;
    @ApiModelProperty("备份名（最近一次）")
    private String lastBackupName;
    @ApiModelProperty("备份文件名（最近一次）")
    private String lastBackupFileName;
    @ApiModelProperty("备份状态（最近一次）")
    private String lastBackupPhase;
    @ApiModelProperty("备份中间件名称")
    private String middlewareCluster;
    @ApiModelProperty("")
    private String controllerName;
    @ApiModelProperty("cron表达式")
    private String schedule;
    @ApiModelProperty("备份文件保留数")
    private Integer keepBackups;
    @ApiModelProperty("创建时间")
    private String creationTimestamp;
}
