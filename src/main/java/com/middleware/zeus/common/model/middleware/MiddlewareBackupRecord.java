package com.middleware.zeus.common.model.middleware;

import com.skyview.language.annotations.DirectTranslate;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 中间件备份记录
 * @author liyinlong
 * @since 2021/9/15 11:43 上午
 */
@Data
public class MiddlewareBackupRecord {

    /**
     * 备份名称
     */
    private String backupName;

    /**
     * cr名称
     */
    private String crName;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 所属备份源
     */
    private String owner;

    /**
     * 服务别名
     */
    private String aliasName;

    /**
     * 备份文件名称
     */
    private String backupFileName;

    /**
     * 备份时间
     */
    private Date backupTime;

    /**
     * 创建时间
     */
    private Date creationTime;

    /**
     * 备份状态
     */
    private String phrase;

    /**
     * 是否停止
     */
    private String pause;

    /**
     * 备份源名称
     */
    private String sourceName;

    /**
     * 备份源类型
     */
    private String sourceType;

    /**
     * 备份类型（Cluster或Pod）
     */
    private String backupType;

    /**
     * 备份方式
     */
    private String backupMode;

    /**
     * pod角色
     */
    private String podRole;

    /**
     * 备份位置
     */
    private String position;

    /**
     * 备份所占存储空间
     */
    private String size;

    /**
     * 备份所占存储空间字节数
     */
    private String byteSize;

    /**
     * 备份位置中文名称
     */
    @Deprecated
    private String addressId;

    /**
     * 备份任务名称
     */
    private String taskName;

    /**
     * 备份任务id
     */
    private String backupId;

    /**
     * 保留时间
     */
    private Integer retentionTime;

    /**
     * 保留个数
     */
    private Integer limitRecord;

    /**
     * 时间单位
     */
    private String dateUnit;

    /**
     * cron表达式
     */
    private String cron;

    /**
     * 使用量
     */
    private String usage;

    /**
     * 失败信息
     */
    private String reason;

    /**
     * 备份记录名称
     */
    private String recordName;

    /**
     * 备份源状态
     */
    private String status;

    /**
     * 是否使用mysqlBackup
     */
    private Boolean mysqlBackup;

    /**
     * 备份位置id
     */
    private String positionId;

    @ApiModelProperty("可用区英文名称(zoneA或zoneB)")
    private String activeArea;

    @ApiModelProperty("可用区别名")
    @DirectTranslate(groupName = "middleware_backup_record", uniqueKeyName = "areaAliasName", keyName = "areaAliasName")
    private String areaAliasName;

    @ApiModelProperty("是否定时备份")
    private Boolean schedule;

    @ApiModelProperty("是否开启增量备份")
    private Boolean increment;

    @ApiModelProperty("n分钟周期")
    private String time;

    @ApiModelProperty("备份地址名称")
    private String backupAddress;

    @ApiModelProperty("是否是双活备份")
    private Boolean activeActive;

    @ApiModelProperty("双活备份信息是否相同")
    private Boolean sameActiveActiveBackup;

    @ApiModelProperty("不应被删除的备份记录")
    private Boolean protect;

    public MiddlewareBackupRecord() {
    }


}
