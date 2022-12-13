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
 * @Date 2022/3/9 10:13 上午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("middleware_param_top")
public class BeanMiddlewareParamTop implements Serializable {

    private static final long serialVersionUID = -895673214672L;

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
     * 名称
     */
    @TableField("name")
    private String name;

    /**
     * 参数名
     */
    @TableField("param")
    private String param;










}
