package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author tangtx
 * @since 2021/3/25 7:55 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("对外路由http路径")
public class IngressHttpPath {
    @ApiModelProperty("服务路径")
    private String path;
    @ApiModelProperty("服务名")
    private String serviceName;
    @ApiModelProperty("服务端口")
    private String servicePort;
}