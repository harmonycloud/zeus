package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 备份服务器详情
 * </p>
 *
 * @author zeus
 * @since 2023-01-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("backup_server_detail")
public class BeanBackupServerDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 备份服务器id
     */
    @TableField("backup_server_id")
    private Integer backupServerId;

    /**
     * 用途：A可用区A,B:可用区B
     */
    @TableField("server_usage")
    private String serverUsage;

    /**
     * 类型：1: S3. 2: ftp: 3: server
     */
    @TableField("type")
    private Integer type;

    /**
     * 协议
     */
    @TableField("protocol")
    private String protocol;

    /**
     * 主机
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
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 服务器类型：1:普通，2:双活
     */
    @TableField(exist = false)
    private Integer serverType;

}
