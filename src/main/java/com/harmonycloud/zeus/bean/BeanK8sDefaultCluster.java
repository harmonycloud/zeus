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
 * @Date 2021/8/3 3:44 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("k8s_default_cluster")
public class BeanK8sDefaultCluster implements Serializable {

    private static final long serialVersionUID = -899876314672L;

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
     * url
     */
    @TableField("url")
    private String url;

    /**
     * service account
     */
    @TableField("token")
    private String token;

}
