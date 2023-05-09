package com.middleware.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author xutianhong
 * @Date 2023/5/8 10:46 上午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("alert_user")
public class BeanAlertUser implements Serializable {

    private static final long serialVersionUID = -2865678135374L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 告警对象类型
     */
    @TableField("alert_type")
    private String alertType;

    /**
     * 集群id
     */
    @TableField("cluster_id")
    private String clusterId;

    /**
     * 分区
     */
    @TableField("namespace")
    private String namespace;

    /**
     * 名称
     */
    @TableField("name")
    private String name;

    /**
     * 是否邮箱告警
     */
    @TableField("mail_alert")
    private Boolean mailAlert;

    /**
     * 是否电话告警
     */
    @TableField("message_alert")
    private Boolean messageAlert;

}
