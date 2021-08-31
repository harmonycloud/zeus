package com.harmonycloud.zeus.bean.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xutianhong
 * @Date 2021/7/27 2:59 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("role_user")
public class BeanUserRole {

    private static final long serialVersionUID = -6579876133L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 名称
     */
    @TableField("username")
    private String userName;
    /**
     * 名称
     */
    @TableField("role_id")
    private Integer roleId;
}


