package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * @author xutianhong
 * @Date 2021/4/6 3:49 下午
 */
@Data
@ApiModel("mysql数据备份")
public class MysqlBackupDto {

    @ApiModelProperty("备份源名称")
    private String name;

    @ApiModelProperty("cr名称")
    private String crName;

    @ApiModelProperty("命名空间")
    private String namespace;

    @ApiModelProperty("备份文件名称")
    private String backupFileName;

    @ApiModelProperty("备份名称")
    private String backupName;

    @ApiModelProperty("日期")
    private Date date;

    @ApiModelProperty("类型")
    private String type;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("位置")
    private String position;

    @ApiModelProperty("备份位置中文名称")
    private String addressName;

    @ApiModelProperty("备份任务名称")
    private String taskName;

    @ApiModelProperty("所属备份")
    private String owner;

}
