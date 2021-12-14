package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author xutianhong
 * @Date 2021/12/13 2:07 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cache_middleware")
public class BeanCacheMiddleware implements Serializable {

    private static final long serialVersionUID = -356732849809L;

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
     * 中间件类型
     */
    @TableField("type")
    private String type;

    /**
     * 中间件版本
     */
    @TableField("chart_version")
    private String chartVersion;

    /**
     * values.yaml
     */
    @TableField("values_yaml")
    private String valuesYaml;



}
