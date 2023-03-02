package com.middleware.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 项目备份服务器关联表
 * </p>
 *
 * @author zeus
 * @since 2023-01-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("project_backup_server")
public class BeanProjectBackupServer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 项目id
     */
    @TableField("project_id")
    private String projectId;

    /**
     * 备份服务器id
     */
    @TableField("backup_server_id")
    private Integer backupServerId;


}
