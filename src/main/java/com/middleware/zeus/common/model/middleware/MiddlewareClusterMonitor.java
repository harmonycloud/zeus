package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@ApiModel("中间件集群的监控")
@Accessors(chain = true)
@Data
public class MiddlewareClusterMonitor implements Serializable {

    private static final long serialVersionUID = -4321567442242679627L;

    @ApiModelProperty("prometheus信息")
    private MiddlewareClusterMonitorInfo prometheus;
    @ApiModelProperty("grafana信息")
    private MiddlewareClusterMonitorInfo grafana;
    @ApiModelProperty("grafana信息")
    private MiddlewareClusterMonitorInfo alertManager;

}
