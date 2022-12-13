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
 * @Date 2022/6/7 10:25 上午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("alert_record")
public class BeanStorageClass implements Serializable {

    private static final long serialVersionUID = -28567198702374L;

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
     * 名称
     */
    @TableField("name")
    private String name;
    /**
     * 中文别名
     */
    @TableField("alias_name")
    private String aliasName;
    /**
     * 存储类型
     */
    @TableField("alias_name")
    private String type;

}
