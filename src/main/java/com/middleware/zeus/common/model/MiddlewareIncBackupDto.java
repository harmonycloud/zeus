package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author xutianhong
 * @Date 2022/8/26 3:35 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "增量备份业务对象")
public class MiddlewareIncBackupDto {

    @ApiModelProperty("所属备份任务id")
    private String backupId;

    @ApiModelProperty("备份名称")
    private String backupName;

    @ApiModelProperty("可用区英文名称(zoneA或zoneB)")
    private String activeArea;

    @ApiModelProperty("可用区别名")
    private String areaAliasName;

    @ApiModelProperty("是否关闭：off/on")
    private String pause;

    @ApiModelProperty("n分钟周期")
    private String time;

    @ApiModelProperty("起始时间")
    private Date startTime;

    @ApiModelProperty("(最新一次备份)结束时间")
    private Date endTime;

    @ApiModelProperty("双活备份信息是否相同")
    private Boolean sameActiveActiveBackup;

}
