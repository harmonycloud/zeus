package com.middleware.zeus.bean.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author xutianhong
 * @Date 2021/7/28 11:18 上午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("resource_menu")
public class BeanResourceMenu implements Serializable {

    private static final long serialVersionUID = -678893223L;

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
     * 字段名称
     */
    @TableField("alias_name")
    private String aliasName;
    /**
     * 字段名称
     */
    @TableField("url")
    private String url;
    /**
     * 字段名称
     */
    @TableField("weight")
    private Integer weight;
    /**
     * 字段名称
     */
    @TableField("icon_name")
    private String iconName;
    /**
     * 字段名称
     */
    @TableField("parent_id")
    private Integer parentId;
    /**
     * 字段名称
     */
    @TableField("module")
    private String module;

}
