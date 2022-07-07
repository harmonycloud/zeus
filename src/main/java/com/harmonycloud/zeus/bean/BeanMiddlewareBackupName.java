package com.harmonycloud.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author yushuaikang
 * @date 2022/6/25 11:10 AM
 */

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("backup_name")
public class BeanMiddlewareBackupName {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 备份任务标识
     */
    @TableField(value = "backup_id")
    private String backupId;

    /**
     * 备份任务名称
     */
    @TableField(value = "backup_name")
    private String backupName;

    /**
     * 备份类型
     */
    @TableField(value = "backup_type")
    private String backupType;

    /**
     * 集群ID
     */
    @TableField(value = "cluster_id")
    private String clusterId;

}
