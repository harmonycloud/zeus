package com.middleware.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * @author xutianhong
 * @Date 2021/11/10 2:59 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cluster_components")
public class BeanClusterComponents implements Serializable {

    private static final long serialVersionUID = -3597343209109L;

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
     * 组件名称
     */
    @TableField("component")
    private String component;

    /**
     * 协议
     */
    @TableField("protocol")
    private String protocol;

    /**
     * 主机地址
     */
    @TableField("host")
    private String host;

    /**
     * 端口
     */
    @TableField("port")
    private String port;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * * 组件状态：0-未安装接入 1-已接入 2-安装中 3-运行正常 4-运行异常 5-卸载中 6-安装失败 7-信息不完整
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

}
