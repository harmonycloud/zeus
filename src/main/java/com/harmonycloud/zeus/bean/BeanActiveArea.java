package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author xutianhong
 * @Date 2022/5/16 10:51 上午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("active_area")
@Accessors(chain = true)
public class BeanActiveArea implements Serializable {

    private static final long serialVersionUID = -8257736782311L;

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
     * 可用区名称
     */
    @TableField("area_name")
    private String areaName;

    /**
     * 可用区别名
     */
    @TableField("alias_name")
    private String aliasName;

    /**
     * 可用区别名
     */
    @TableField("init")
    private Boolean init;

}
