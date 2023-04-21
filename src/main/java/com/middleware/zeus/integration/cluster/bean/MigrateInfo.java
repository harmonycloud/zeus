package com.middleware.zeus.integration.cluster.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author wangpenglei
 * @Date 2023/1/15 下午8:12
 **/
@Accessors(chain = true)
@Data
@ApiModel("记录迁移状态信息")
public class MigrateInfo {
    @ApiModelProperty("迁移状态")
    private String status;

    @ApiModelProperty("迁移失败原因")
    private String reason;

    @ApiModelProperty("迁移时间")
    private Date migrateTimestamp;

    @ApiModelProperty("maintenance名称")
    private String mtName;
}
