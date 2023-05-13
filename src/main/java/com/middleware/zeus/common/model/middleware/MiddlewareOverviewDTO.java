package com.middleware.zeus.common.model.middleware;

import com.middleware.zeus.common.model.MiddlewareDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 中间件总览实体类
 * @author chenzhiling
 * @Date 2021/4/29 11:31 上午
 */
@ApiModel("中间件总览实体类")
@Accessors(chain = true)
@Data
public class MiddlewareOverviewDTO {

    @ApiModelProperty("集群数")
    private Integer totalClusterCount = 0;

    @ApiModelProperty("总分区数")
    private Integer totalNamespaceCount = 0;

    @ApiModelProperty("总实例数")
    private Integer totalInstanceCount = 0;

    @ApiModelProperty("异常实例数")
    private Integer totalExceptionCount = 0;

    @ApiModelProperty("集群详细")
    private List<OverviewClusterInfo> clusters;

    @ApiModelProperty("实例详细")
    private List<MiddlewareDTO> middlewareDTOList;
}
