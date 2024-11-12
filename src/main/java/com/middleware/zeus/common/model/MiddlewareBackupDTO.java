package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2021/11/4 4:46 下午
 */
@Accessors(chain = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiddlewareBackupDTO {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区名称")
    private String namespace;

    @ApiModelProperty("中间件名称")
    private String middlewareName;

    @ApiModelProperty("组织id")
    private String organId;

    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("中间件类型")
    private String type;

    @ApiModelProperty("备份任务id")
    private String backupId;

    @ApiModelProperty("备份任务名称")
    private String taskName;

    @ApiModelProperty("备份任务类型(1：普通备份，2：双活备份)，此字段前端不用传")
    private Integer taskType;

    @ApiModelProperty("cron表达式")
    private String cron;

    @ApiModelProperty("保留时间")
    private Integer retentionTime;

    @ApiModelProperty("时间单位")
    private String dateUnit;

    @ApiModelProperty("备份服务器id")
    private Integer backupServerId;

    @ApiModelProperty("备份保留个数")
    private Integer limitRecord;

    @ApiModelProperty("pod名称")
    private List<String> pods;

    @ApiModelProperty("是否停止定时备份 on:停止 off:不停止")
    private String pause;

    @ApiModelProperty("中间件crd类型 例：mysqlcluster、kafkacluster")
    private String crdType;

    @ApiModelProperty("中间件备份的label")
    Map<String, String> labels;

    @ApiModelProperty("中间件备份的annotations")
    Map<String, String> annotations;

    @ApiModelProperty("中间件定时备份记录名称")
    private String backupName;

    @ApiModelProperty("是否使用mysqlBackup")
    private Boolean mysqlBackup;

    @ApiModelProperty("是否开启增量备份")
    private Boolean increment;

    @ApiModelProperty("是否关闭增量备份")
    private Boolean turnOff;

    @ApiModelProperty("n分钟周期")
    private String time;

    @ApiModelProperty("双活可用区备份信息是否相同")
    private Boolean sameActiveActiveBackup;

}
