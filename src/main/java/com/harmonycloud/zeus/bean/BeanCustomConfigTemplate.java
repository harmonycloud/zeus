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
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 模板uid
     */
    @TableField("uid")
    private String uid;

    /**
     * 模板名称
     */
    @TableField("name")
    private String name;

    /**
     * 模板名称
     */
    @TableField("description")
    private String description;

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

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

}