package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * @author dengyulong
 * @date 2021/05/24
 */
@ApiModel("集群日志信息")
@Accessors(chain = true)
@Data
public class MiddlewareClusterLoggingInfo implements Serializable {

    private static final long serialVersionUID = -5375458811500584251L;

    @ApiModelProperty("协议")
    private String protocol;
    @ApiModelProperty("地址")
    private String host;
    @ApiModelProperty("端口")
    private String port;
    @ApiModelProperty("用户")
    private String user;
    @ApiModelProperty("密码")
    private String password;
    @ApiModelProperty("日志采集组件")
    private Boolean logCollect;
    @ApiModelProperty("日志保留时间")
    private Integer logKeepDays;

    public String getAddress() {
        return this.protocol + "://" + this.host + (StringUtils.isEmpty(this.port) ? "" : ":" + this.port);
    }
    
}
