package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2023/5/6 11:19 上午
 */
@Data
@Accessors(chain = true)
@ApiModel("告警记录查询")
public class AlertRecordQueryDto {

    @ApiModelProperty("告警等级")
    private String alertLevel;

    @ApiModelProperty("告警对象")
    private String alertTarget;

    @ApiModelProperty("关键词查询")
    private String keyword;

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("中间件类型")
    private String middlewareType;

    @ApiModelProperty("告警时间排序: asc/desc")
    private String alertTime;

    @ApiModelProperty("接收时间排序: asc/desc")
    private String receiveTime;

    @ApiModelProperty("当前页")
    private Integer current;

    @ApiModelProperty("每页记录数")
    private Integer size;

    @ApiModelProperty("告警记录类型: system/cluster/service")
    private String alertType;



}
