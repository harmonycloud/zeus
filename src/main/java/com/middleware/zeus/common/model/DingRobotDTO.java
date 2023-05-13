package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author yushuaikang
 * @date 2021/11/24 下午3:03
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "钉钉机器人")
public class DingRobotDTO {

    @ApiModelProperty("webhook")
    private String webhook;

    @ApiModelProperty("密钥")
    private String secret;

    @ApiModelProperty("连接测试")
    private boolean isSuccess;
}
