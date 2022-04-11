package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author yushuaikang
 * @date 2022/3/10 上午10:45
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("image_repository")
public class BeanImageRepository {

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 集群ID
     */
    @TableField(value = "cluster_id")
    private String clusterId;

    /**
     * 协议
     */
    @TableField(value = "protocol")
    private String protocol;

    /**
     * harbor地址
     */
    @TableField(value = "address")
    private String address;

    /**
     * harbor主机地址
     */
    @TableField(value = "host_address")
    private String hostAddress;

    /**
     * 端口号
     */
    @TableField(value = "port")
    private Integer port;

    /**
     * harbor项目
     */
    @TableField(value = "project")
    private String project;

    /**
     * 用户名
     */
    @TableField(value = "username")
    private String username;

    /**
     * 密码
     */
    @TableField(value = "password")
    private String password;

    /**
     * 描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 是否默认
     */
    @TableField(value = "is_default")
    private Integer isDefault;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;
}
