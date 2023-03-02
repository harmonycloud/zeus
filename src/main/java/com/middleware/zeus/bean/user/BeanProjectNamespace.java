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
 * @Date 2022/3/25 11:30 上午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("project_namespace")
public class BeanProjectNamespace implements Serializable {

    private static final long serialVersionUID = -893624875721973864L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 项目id
     */
    @TableField("project_id")
    private String projectId;
    /**
     * 分区名称
     */
    @TableField("namespace")
    private String namespace;
    /**
     * 分区别名
     */
    @TableField("alias_name")
    private String aliasName;
    /**
     * 集群id
     */
    @TableField("cluster_id")
    private String clusterId;


}
