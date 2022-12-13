package com.middleware.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 告警设置
 * </p>
 *
 * @author liyinlong
 * @since 2022-07-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("alert_setting")
public class BeanAlertSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 集群id
     */
    @TableField("cluster_id")
    private String clusterId;

    /**
     * 分区名称
     */
    @TableField("namespace")
    private String namespace;

    /**
     * 服务名称
     */
    @TableField("middleware_name")
    private String middlewareName;

    /**
     * 告警等级 service:服务告警 system:系统告警
     */
    @TableField("lay")
    private String lay;

    /**
     * 是否开启钉钉告警
     */
    @TableField("enable_ding_alert")
    private String enableDingAlert;

    /**
     * 是否开启邮件告警
     */
    @TableField("enable_mail_alert")
    private String enableMailAlert;


}
