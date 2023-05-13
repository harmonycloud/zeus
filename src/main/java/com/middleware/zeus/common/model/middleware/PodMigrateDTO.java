package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author wangpenglei
 * @Date 2023/1/10 下午3:31
 **/
@Accessors(chain = true)
@Data
@ApiModel("pod迁移信息")
public class PodMigrateDTO {

    @ApiModelProperty("迁移记录名")
    private String name;

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("命名空间")
    private String nameSpace;

    @ApiModelProperty("迁移pod名")
    private String podName;

    @ApiModelProperty("目标主机名")
    private String targetHost;

    @ApiModelProperty("迁移时间")
    private Date migrateTimestamp;

    @ApiModelProperty("迁移状态")
    private String status;

    @ApiModelProperty("失败原因")
    private String reason;
}
