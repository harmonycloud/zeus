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
 * @Date 2021/11/29 4:57 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cluster_ingress_components")
public class BeanIngressComponents implements Serializable {

    private static final long serialVersionUID = -678907123413L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * ingress name
     */
    @TableField("name")
    private String name;
    /**
     * ingress class name
     */
    @TableField("ingress_class_name")
    private String ingressClassName;
    /**
     * 集群Id
     */
    @TableField("cluster_id")
    private String clusterId;
    /**
     * 状态 0-未安装接入 1-已接入 2-安装中 3-运行正常 4-运行异常 5-卸载中 6-安装异常
     */
    @TableField("status")
    private Integer status;
}
