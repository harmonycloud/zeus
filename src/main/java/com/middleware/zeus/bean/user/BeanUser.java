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
 * @Date 2021/7/27 10:11 上午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user")
public class BeanUser implements Serializable {

    private static final long serialVersionUID = -6897323133L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 账户
     */
    @TableField("username")
    private String userName;
    /**
     * 用户名
     */
    @TableField("alias_name")
    private String aliasName;
    /**
     * 密码
     */
    @TableField("password")
    private String password;
    /**
     * 邮箱
     */
    @TableField("email")
    private String email;
    /**
     * 电话
     */
    @TableField("phone")
    private String phone;
    /**
     * 创建者
     */
    @TableField("creator")
    private String creator;
    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;
    /**
     * 密码修改时间
     */
    @TableField("password_time")
    private Date passwordTime;
}
