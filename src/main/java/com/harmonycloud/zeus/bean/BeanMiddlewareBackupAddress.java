package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author yushuaikang
 * @Date 2022/6/9 9:54 上午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("middleware_backup_address")
public class BeanMiddlewareBackupAddress {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 中文名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 类型
     */
    @TableField(value = "type")
    private String type;

    /**
     * id
     */
    @TableField(value = "relevance_num")
    private Integer relevanceNum;

    /**
     * 关联任务数
     */
    @TableField(value = "status")
    private Integer status;

    //S3相关
    /**
     * bucket名称
     */
    @TableField(value = "bucket_name")
    private String bucketName;

    /**
     * 用户ID
     */
    @TableField(value = "access_key_id")
    private String accessKeyId;

    /**
     * 密码
     */
    @TableField(value = "secret_access_key")
    private String secretAccessKey;

    /**
     * 容量
     */
    @TableField(value = "capacity")
    private String capacity;

    /**
     * 地址
     */
    @TableField(value = "endpoint")
    private String endpoint;

    //FTP相关
    /**
     * FTP主机服务器
     */
    @TableField(value = "ftp_host")
    private String ftpHost;

    /**
     * FTP登录用户名
     */
    @TableField(value = "ftp_user")
    private String ftpUser;

    /**
     * FTP登录密码
     */
    @TableField(value = "ftp_password")
    private String ftpPassword;

    /**
     * FTP端口
     */
    @TableField(value = "ftp_port")
    private Integer ftpPort;

    //服务器相关
    /**
     * 服务器地址
     */
    @TableField(value = "server_host")
    private String serverHost;

    /**
     * 服务器用户名
     */
    @TableField(value = "server_user")
    private String serverUserName;

    /**
     * 服务器密码
     */
    @TableField(value = "server_password")
    private String serverPassword;

    /**
     * 服务器端口
     */
    @TableField(value = "server_port")
    private Integer serverPort;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

}
