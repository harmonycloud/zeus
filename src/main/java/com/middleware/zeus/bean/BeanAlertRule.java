package com.middleware.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author xutianhong
 * @Date 2021/5/26 11:31 上午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("alert_rule")
public class BeanAlertRule implements Serializable {

    private static final long serialVersionUID = -3567432809L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 包名
     */
    @TableField("chart_name")
    private String chartName;

    /**
     * 包名
     */
    @TableField("chart_version")
    private String chartVersion;

    /**
     * alert
     */
    @TableField("alert")
    private String alert;
}
