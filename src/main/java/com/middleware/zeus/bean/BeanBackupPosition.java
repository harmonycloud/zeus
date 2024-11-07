package com.middleware.zeus.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 备份位置表
 * </p>
 *
 * @author zeus
 * @since 2023-01-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("backup_position")
public class BeanBackupPosition implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 备份位置名称
     */
    @TableField("name")
    private String name;

    /**
     * 组织id
     */
    @TableField("organ_id")
    private String organId;

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

    /**
     * 备份路径（对于minio则是bucket）
     */
    @TableField("backup_position")
    private String backupPosition;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 备份服务器详情id
     */
    @TableField("backup_server_detail_id")
    private Integer backupServerDetailId;


}
