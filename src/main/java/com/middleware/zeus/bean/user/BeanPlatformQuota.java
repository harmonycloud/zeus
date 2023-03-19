package com.middleware.zeus.bean.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author xutianhong
 * @Date 2023/3/6 5:14 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("platform_quota")
public class BeanPlatformQuota implements Serializable {

    private static final long serialVersionUID = -67678126388932L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * uid
     */
    @TableField("uid")
    private String uid;
    /**
     * 类型: organ,project
     */
    @TableField("type")
    private String type;
    /**
     * 集群id
     */
    @TableField("cluster_id")
    private String clusterId;
    /**
     * 资源对象: cpu memory storage
     */
    @TableField("target")
    private String target;
    /**
     * 资源对象名称: cpu memory storageName
     */
    @TableField("name")
    private String name;
    /**
     * 配额
     */
    @TableField("quota")
    private Double quota;

}
