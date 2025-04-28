package com.middleware.zeus.bean;

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
 * @Date 2025/4/28 上午9:56
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("middleware_disable_version")
public class BeanMiddlewareDisableVersion implements Serializable  {

    private static final long serialVersionUID = -672867192422L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 集群id
     */
    @TableField("cluster_id")
    private String clusterId;

    /**
     * 中间件类型
     */
    @TableField("chart_name")
    private String chartName;

    /**
     * 中间件版本
     */
    @TableField("chart_version")
    private String chartVersion;

    /**
     * 禁用版本
     */
    @TableField("disable_version")
    private String disableVersion;

}
