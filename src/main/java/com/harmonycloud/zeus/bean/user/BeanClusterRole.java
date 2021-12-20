package com.harmonycloud.zeus.bean.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author xutianhong
 * @Date 2021/12/13 5:53 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cluster_role")
public class BeanClusterRole implements Serializable {

    private static final long serialVersionUID = -67567130123L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 角色id
     */
    @TableField("role_id")
    private Integer roleId;
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



}
