package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author dengyulong
 * @date 2021/03/24
 */
@ApiModel("中间件备份信息")
@Accessors(chain = true)
@Data
public class MiddlewareBackup implements Serializable {

    private static final long serialVersionUID = 4449154754111442278L;

    @ApiModelProperty("类型")
    private String type;
    @ApiModelProperty("存储服务名称")
    private String storageClassName;
    @ApiModelProperty("存储服务类型")
    private String storageClassType;
    @ApiModelProperty("节点ip")
    private String nodeIp;
    @ApiModelProperty("状态")
    private String status;
    @ApiModelProperty("备份大小")
    private String size;
    @ApiModelProperty("开始时间")
    private String startTime;
    @ApiModelProperty("结束时间")
    private String endTime;
    @ApiModelProperty("保留天数")
    private Integer remainDay;
    @ApiModelProperty("cron表达式")
    private String cron;

}
