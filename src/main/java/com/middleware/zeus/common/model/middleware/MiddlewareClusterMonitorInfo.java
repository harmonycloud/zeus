package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@ApiModel("中间件集群的监控信息")
@Accessors(chain = true)
@Data
public class MiddlewareClusterMonitorInfo implements Serializable {

    private static final long serialVersionUID = 8474871315062825271L;

    @ApiModelProperty("协议")
    private String protocol;
    @ApiModelProperty("域名/IP")
    private String host;
    @ApiModelProperty("端口")
    private String port;
    @ApiModelProperty("token")
    private String token;
    @ApiModelProperty("address")
    private String address;
    @ApiModelProperty("username")
    private String username;
    @ApiModelProperty("password")
    private String password;
    @ApiModelProperty("silentTime")
    private String silentTime;

    public String getAddress() {
        return StringUtils.isEmpty(address)
                ? this.protocol + "://" + this.host + (StringUtils.isEmpty(this.port) ? "" : ":" + this.port) : address;
    }

}
