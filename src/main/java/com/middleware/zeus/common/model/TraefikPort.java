package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2022/10/22 4:10 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "traefik端口")
public class TraefikPort {

    @ApiModelProperty("起始服务端口(traefik专有)")
    private Integer startPort;
    @ApiModelProperty("结束服务端口(traefik专有)")
    private Integer endPort;

}
