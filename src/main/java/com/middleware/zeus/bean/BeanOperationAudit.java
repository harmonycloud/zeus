package com.middleware.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 * 操作审计
 * </p>
 *
 * @author liyinlong
 * @since 2021-07-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("operation_audit")
public class BeanOperationAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 账户名称
     */
    @TableField("account")
    private String account;

    /**
     * 用户名称
     */
    @TableField("user_name")
    private String userName;

    /**
     * 角色名称
     */
    @TableField("role_name")
    private String roleName;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * url
     */
    @TableField("url")
    private String url;

    /**
     * 模块名称
     */
    @TableField("module_ch_desc")
    private String moduleChDesc;

    /**
     * 子模块名称
     */
    @TableField("child_module_ch_desc")
    private String childModuleChDesc;

    /**
     * 操作名称
     */
    @TableField("action_ch_desc")
    private String actionChDesc;

    /**
     * 方法
     */
    @TableField("method")
    private String method;

    /**
     * 请求方法类型
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * 请求参数
     */
    @TableField("request_params")
    private String requestParams;

    /**
     * 响应内容
     */
    @TableField("response")
    private String response;

    /**
     * 请求ip
     */
    @TableField("remote_ip")
    private String remoteIp;

    /**
     * 状态码
     */
    @TableField("status")
    private String status;

    /**
     * 请求开始时间
     */
    @TableField("begin_time")
    private Date beginTime;

    /**
     * 请求响应时间
     */
    @TableField("action_time")
    private Date actionTime;

    /**
     * 执行时长(ms)
     */
    @TableField("execute_time")
    private Integer executeTime;

    /**
     * 集群id
     */
    @TableField("cluster_id")
    private String clusterId;

    /**
     * 请求token
     */
    @TableField("token")
    private String token;


}
