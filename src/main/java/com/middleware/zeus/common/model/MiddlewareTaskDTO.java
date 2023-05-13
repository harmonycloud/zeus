package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/31 2:18 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("中间件备份任务信息")
public class MiddlewareTaskDTO {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区名称")
    private String namespace;

    @ApiModelProperty("中间件类型")
    private String type;

    @ApiModelProperty("备份任务ID")
    private String backupId;

    @ApiModelProperty("是否是周期备份")
    private String backupMode;

    @ApiModelProperty("备份任务名称列表")
    private List<String> backupNameList;

}
