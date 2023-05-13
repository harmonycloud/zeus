package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author chwetion
 * @since 2021/1/15 2:49 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("负载均衡域名")
public class LoadbalancerDomain {
    @ApiModelProperty("域名")
    private String domain;
    @ApiModelProperty("关联服务")
    private List<String> relatedServiceName;
}
