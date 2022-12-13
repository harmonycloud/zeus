package com.middleware.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author yushuaikang
 * @Date 2022/6/15 9:54 上午
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("backup_address_cluster")
public class BeanMiddlewareBackupToCluster {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 备份地址ID
     */
    @TableField(value = "backup_address_id")
    private Integer backupAddressId;

    /**
     * 集群ID
     */
    @TableField(value = "cluster_id")
    private String clusterId;

}
