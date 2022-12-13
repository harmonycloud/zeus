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
 * @Date 2021/4/26 2:41 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("custom_config_history")
public class BeanCustomConfigHistory implements Serializable {

    private static final long serialVersionUID = -23436323253L;

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
     * 分区
     */
    @TableField("namespace")
    private String namespace;

    /**
     * 中间件名称
     */
    @TableField("name")
    private String name;

    /**
     * 修改项
     */
    @TableField("item")
    private String item;

    /**
     * 修改前
     */
    @TableField("last")
    private String last;

    /**
     * 修改后
     */
    @TableField("after")
    private String after;

    /**
     * 是否需要重启
     */
    @TableField("restart")
    private Boolean restart;

    /**
     * 是否已启用
     */
    @TableField("status")
    private Boolean status;

    /**
     * 修改日期
     */
    @TableField("date")
    private Date date;

}
