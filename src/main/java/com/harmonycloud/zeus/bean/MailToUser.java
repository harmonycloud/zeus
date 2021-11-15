package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author yushuaikang
 * @date 2021/11/11 下午3:53
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mail_to_user")
public class MailToUser {

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
     * 创建时间
     */
    @TableField("create_time")
    private Date time;

}
