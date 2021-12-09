package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * @author xutianhong
 * @Date 2021/4/27 4:53 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("alert_record")
public class BeanAlertRecord implements Serializable {

    private static final long serialVersionUID = -2865374L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 集群id
     */
    @TableField("cluster_id")
    private String clusterId;
    /**
     * 分区
     */
    @TableField("namespace")
    private String namespace;
    /**
     * 中间件名称
     */
    @TableField("name")
    private String name;
    /**
     * 中间件类型
     */
    @TableField("type")
    private String type;
    /**
     * 规则名称
     */
    @TableField("alert")
    private String alert;
    /**
     * 简讯
     */
    @TableField("summary")
    private String summary;
    /**
     * 描述
     */
    @TableField("message")
    private String message;
    /**
     * 事件等级
     */
    @TableField("level")
    private String level;
    /**
     * 告警时间
     */
    @TableField("time")
    private Date time;

    /**
     * 告警层面
     */
    @TableField("lay")
    private String lay;

    /**
     * 规则ID
     */
    @TableField("alert_id")
    private Integer alertId;

    /**
     * 规则描述
     */
    @TableField("expr")
    private String expr;

    /**
     * 告警内容
     */
    @TableField("content")
    private String content;

}
