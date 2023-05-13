package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/9 11:42 上午
 */
@Accessors(chain = true)
@Data
@ApiModel("实时监控接口")
public class MonitorDto {

    @ApiModelProperty("链接")
    private String url;
    @ApiModelProperty("token")
    private String authorization;

}
