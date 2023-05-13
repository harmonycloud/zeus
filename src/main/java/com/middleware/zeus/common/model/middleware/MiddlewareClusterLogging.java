package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author dengyulong
 * @date 2021/05/24
 */
@ApiModel("集群日志")
@Accessors(chain = true)
@Data
public class MiddlewareClusterLogging implements Serializable {

    private static final long serialVersionUID = 3161109315325917989L;

    @ApiModelProperty("es配置")
    private MiddlewareClusterLoggingInfo elasticSearch;

}
