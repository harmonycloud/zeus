package com.middleware.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author yushuaikang
 * @date 2021/11/8 下午4:14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mail_info")
public class MailInfo {

    @TableId (value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField(value = "mail_server")
    private String mailServer;

    @TableField(value = "port")
    private int port;

    @TableField(value = "username")
    private String userName;

    @TableField(value = "password")
    private String password;

    @TableField(value = "mail_path")
    private String mailPath;

    @TableField(value = "creat_time")
    private String time;

}

