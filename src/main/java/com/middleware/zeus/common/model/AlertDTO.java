package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author xutianhong
 * @Date 2021/4/29 10:28 上午
 */
@Accessors(chain = true)
@Data
@ApiModel("告警信息")
public class AlertDTO {

    @ApiModelProperty("组织id")
    private String organId;

    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("集群别名")
    private String nickname;

    @ApiModelProperty("分区")
    private String namespace;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("别名")
    private String aliasName;

    @ApiModelProperty("中间件类型")
    private String type;

    @ApiModelProperty("中间件类型(大写)")
    private String capitalType;

    @ApiModelProperty("告警名称")
    private String alert;

    @ApiModelProperty("告警等级")
    private String level;

    @ApiModelProperty("告警信息")
    private String message;

    @ApiModelProperty("告警简讯")
    private String summary;

    @ApiModelProperty("告警触发时间")
    private Date alertTime;

    @ApiModelProperty("告警接收时间")
    private Date alertReceiveTime;

    @ApiModelProperty("chart版本")
    private String chartVersion;

    @ApiModelProperty("告警层面")
    private String lay;

    @ApiModelProperty("告警类型")
    private String alertType;

    @ApiModelProperty("规则描述")
    private String expr;

    @ApiModelProperty("告警ID")
    private String alertId;

    @ApiModelProperty("告警内容")
    private String content;

}
