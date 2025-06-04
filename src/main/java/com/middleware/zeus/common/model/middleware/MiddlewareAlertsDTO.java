package com.middleware.zeus.common.model.middleware;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/26 10:24 上午
 */
@Data
@Accessors(chain = true)
@ApiModel("中间件告警规则")
public class MiddlewareAlertsDTO {

    @ApiModelProperty("规则ID")
    private String alertId;

    @ApiModelProperty("集群ID")
    private String clusterId;

    @ApiModelProperty("集群别名")
    private String nickname;

    @ApiModelProperty("命名空间")
    private String namespace;

    @ApiModelProperty("中间件名称")
    private String middlewareName;

    @ApiModelProperty("规则名称")
    private String alert;

    @ApiModelProperty("告警内容")
    private String content;

    @ApiModelProperty("告警层面 system 系统告警 service 服务告警")
    private String lay;

    @ApiModelProperty("告警等级")
    private String level;

    @ApiModelProperty("中间件名称")
    private String name;

    @ApiModelProperty("中间件类型")
    private String type;

    @ApiModelProperty("告警模式")
    private String alertMode;

    @ApiModelProperty("执行规则")
    private String expr;

    @ApiModelProperty("监控项(规则中文名)")
    private String description;

    @ApiModelProperty("符号：>,<,=")
    private String symbol;

    @ApiModelProperty("阈值")
    private String threshold;

    @ApiModelProperty("沉默时间")
    private String silence;

    @ApiModelProperty("n分钟周期")
    private String time;

    @ApiModelProperty("备注")
    private Map<String, String> annotations;

    @ApiModelProperty("标签")
    private Map<String, String> labels;

    @ApiModelProperty("完全匹配")
    private Map<String, String> exactMatch;

    @ApiModelProperty("模糊匹配")
    private Map<String, String> fuzzyMatch;

    @ApiModelProperty("单位")
    private String unit;

    @ApiModelProperty("规则时间")
    private BigDecimal alertTime;

    @ApiModelProperty("规则次数")
    private BigDecimal alertTimes;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("是否选择钉钉")
    private String ding;

    @ApiModelProperty("是否选择邮箱")
    private String mail;

    @ApiModelProperty("ip")
    private String ip;

    @ApiModelProperty("自定义规则")
    private Boolean custom;

}
