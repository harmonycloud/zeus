package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author xutianhong
 * @Date 2021/5/10 2:34 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("custom_config")
public class BeanCustomConfig implements Serializable {

    private static final long serialVersionUID = -6783489213L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 字段名称
     */
    @TableField("name")
    private String name;
    /**
     * chart包名称
     */
    @TableField("chart_name")
    private String chartName;
    /**
     * chart包版本
     */
    @TableField("chart_version")
    private String chartVersion;
    /**
     * 默认值
     */
    @TableField("default_value")
    private String defaultValue;
    /**
     * 是否重启
     */
    @TableField("restart")
    private Boolean restart;
    /**
     * 值域
     */
    @TableField("ranges")
    private String ranges;
    /**
     * 正则校验
     */
    @TableField("pattern")
    private String pattern;
    /**
     * 描述
     */
    @TableField("description")
    private String description;

}
