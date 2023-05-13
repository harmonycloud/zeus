package com.middleware.zeus.common.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2021/1/4 12:19 上午
 */
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("openyurt-nginx")
public class OpenYurtNginxLoadbalancer extends Loadbalancer {
    @ApiModelProperty("负载均衡属性")
    private OpenYurtNginxLoadbalancerAttributes attributes;
}
