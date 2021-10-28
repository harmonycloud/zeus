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
 * @Date 2021/9/3 4:05 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cluster_middleware_info")
public class BeanClusterMiddlewareInfo implements Serializable {

    private static final long serialVersionUID = -82637862811L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 集群id
     */
    @TableField("cluster_Id")
    private String clusterId;

    /**
     * chart名称
     */
    @TableField("chart_name")
    private String chartName;

    /**
     * chart版本
     */
    @TableField("chart_version")
    private String chartVersion;

    /**
     * 状态
     */
    @TableField("status")
    private Integer status;
}
