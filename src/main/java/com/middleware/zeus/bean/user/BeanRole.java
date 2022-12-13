package com.middleware.zeus.bean.user;

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
 * @Date 2021/7/27 2:35 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("role")
public class BeanRole implements Serializable {

    private static final long serialVersionUID = -656783133L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 名称
     */
    @TableField("name")
    private String name;
    /**
     * 描述
     */
    @TableField("description")
    private String description;
    /**
     * 父id
     */
    @TableField("parent")
    private Integer parent;
    /**
     * 创建时间
     */
    @TableField("create_Time")
    private Date createTime;
    /**
     * 是否可用
     */
    @TableField("status")
    private Boolean status;
}
