package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author yushuaikang
 * @date 2021/11/12 下午4:19
 */
@Data
@Accessors(chain = true)
@ApiModel("中间件告警信息")
public class AlertInfoDto {

    @ApiModelProperty("规则ID")
    private String ruleID;

    @ApiModelProperty("告警对象")
    private String clusterId;

    @ApiModelProperty("中间件名称")
    private String middlewareName;

    @ApiModelProperty("告警内容")
    private String content ;

    @ApiModelProperty("实际监测")
    private String message;

    @ApiModelProperty("监控项(规则中文名)")
    private String description;

    @ApiModelProperty("告警等级")
    private String level;

    @ApiModelProperty("ip")
    private String ip;

    @ApiModelProperty("告警时间")
    private Date alertTime;


}
