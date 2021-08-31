package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author xutianhong
 * @Date 2021/4/25 11:35 上午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("custom_config_template")
public class BeanCustomConfigTemplate implements Serializable {

    private static final long serialVersionUID = -89773467354672L;

    /**
     * 自增id
     */
    @TableId("id")
    private Integer id;

    /**
     * 模板名称
     */
    @TableField("name")
    private String name;

    /**
     * 模板中文名称
     */
    @TableField("alias_name")
    private String aliasName;

    /**
     * 中间件类型
     */
    @TableField("type")
    private String type;

    /**
     * 配置内容
     */
    @TableField("config")
    private String config;

}