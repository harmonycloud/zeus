package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2021/1/15 9:55 上午
 */
@Accessors(chain = true)
@Data
@ApiModel("对外路由指定服务")
public class IngressService {
    @ApiModelProperty("服务路径")
    private String uri;
    @ApiModelProperty("服务名")
    private String serviceName;
    @ApiModelProperty("服务端口")
    private String servicePort;

    public IngressService(String uri, String serviceName, String servicePort) {
        this.uri = uri;
        this.serviceName = serviceName;
        this.servicePort = servicePort;
    }
}
