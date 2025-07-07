package com.middleware.zeus.common.model.middleware;

import com.middleware.zeus.bean.BeanMiddlewareDisableVersion;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2025/4/28 上午10:25
 */
@Data
@NoArgsConstructor
@ApiModel("中间件禁用版本")
@Accessors(chain = true)
@AllArgsConstructor
public class MiddlewareDisableVersionDo {

    @ApiModelProperty("chart名称")
    private String chartName;

    @ApiModelProperty("chart版本")
    private String chartVersion;

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("版本")
    private String version;

    @ApiModelProperty("是否可用")
    private Boolean enable;

    public MiddlewareDisableVersionDo(BeanMiddlewareDisableVersion bean) {
        this.chartName = bean.getChartName();
        this.chartVersion = bean.getChartVersion();
        this.clusterId = bean.getClusterId();
        this.version = bean.getDisableVersion();
        this.enable = false;
    }

}
