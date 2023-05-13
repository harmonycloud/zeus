package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author xutianhong
 * @Date 2023/5/9 11:08 上午
 */
@Accessors(chain = true)
@Data
@ApiModel("告警记录")
public class AlertRecordDo {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区")
    private String namespace;

    @ApiModelProperty("告警对象名称")
    private String targetName;

    @ApiModelProperty("告警对象别名")
    private String targetAliasName;

    @ApiModelProperty("中间件类型")
    private String middlewareType;

    @ApiModelProperty("告警类型")
    private String alertType;

    @ApiModelProperty("告警规则名称")
    private String alertName;

    @ApiModelProperty("告警等级")
    private String level;

    @ApiModelProperty("告警信息")
    private String message;

    @ApiModelProperty("告警概要")
    private String summary;

    @ApiModelProperty("沉默时间")
    private String silence;

    @ApiModelProperty("阈值触发时间")
    private String time;

    @ApiModelProperty("告警规则")
    private String expr;

    @ApiModelProperty("告警触发时间")
    private Date alertTime;

    @ApiModelProperty("告警接收时间")
    private Date alertReceiveTime;

}
