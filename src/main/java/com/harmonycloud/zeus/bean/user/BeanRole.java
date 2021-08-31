package com.harmonycloud.zeus.bean.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xutianhong
 * @Date 2021/7/27 2:35 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("role")
public class BeanRole {

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
     * 是否可用
     */
    @TableField("status")
    private String status;
}
