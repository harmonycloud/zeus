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
 * @Date 2023/3/13 3:15 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("organization_backup_server")
public class BeanOrganizationBackupServer implements Serializable {

    private static final long serialVersionUID = -6897682364178L;

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
     * 组织id
     */
    @TableField("organ_id")
    private String organId;
    /**
     * 备份服务器id
     */
    @TableField("backup_server_id")
    private Integer backupServerId;

}
