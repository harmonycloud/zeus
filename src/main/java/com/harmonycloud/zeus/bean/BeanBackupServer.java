package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 备份服务器
 * </p>
 *
 * @author zeus
 * @since 2023-01-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("backup_server")
public class BeanBackupServer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 备份服务器名称
     */
    @TableField("name")
    private String name;

    /**
     * 所属集群名称
     */
    @TableField("cluster_id")
    private String clusterId;

    /**
     * 备份服务器类型（1:普通，2:双活）
     */
    @TableField("type")
    private Integer type;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;


}
