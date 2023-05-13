package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @auther wangpenglei
 * @date 2023/3/22 19:41
 */
@Accessors(chain = true)
@Data
public class DisasterRecoveryInfo {
    @ApiModelProperty("协议")
    private String protocol;
    @ApiModelProperty("端口号")
    private Integer port;
    @ApiModelProperty("主机")
    private String host;
    @ApiModelProperty("是否远程")
    private Boolean isRelation;
    @ApiModelProperty("名字")
    private String name;
    @ApiModelProperty("运行状态")
    private String phase;
}
