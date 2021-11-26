package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author yushuaikang
 * @date 2021/11/17 上午11:07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("alert_rule_id")
public class MiddlewareAlertInfo {

    @TableId(value = "alert_id", type = IdType.AUTO)
    private Integer alertId;

    @TableField("cluster_id")
    private String clusterId;

    @TableField("namespace")
    private String namespace;

    @TableField("middleware_name")
    private String middlewareName;

    @TableField("id")
    private String id;

    @TableField("enable")
    private String enable;

    @TableField("alert")
    private String alert;

    @TableField("content")
    private String content;

    @TableField("lay")
    private String lay;

    @TableField("name")
    private String name;

    @TableField("type")
    private String type;

    @TableField("expr")
    private String expr;

    @TableField("status")
    private String status;

    @TableField("description")
    private String description;

    @TableField("symbol")
    private String symbol;

    @TableField("threshold")
    private String threshold;

    @TableField("silence")
    private String silence;

    @TableField("time")
    private String time;

    @TableField("annotations")
    private String annotations;

    @TableField("labels")
    private String labels;

    @TableField("unit")
    private String unit;

    @TableField("alert_time")
    private BigDecimal alertTime;

    @TableField("alert_times")
    private BigDecimal alertTimes;

    @TableField("create_time")
    private Date createTime;


}
