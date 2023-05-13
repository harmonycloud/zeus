package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author tangtx
 * @since 2021/3/25 7:55 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("对外路由规则")
public class IngressRuleDTO {

    @ApiModelProperty("域名")
    private String domain;

    @ApiModelProperty("http路由路径")
    private List<IngressHttpPath> ingressHttpPaths;

    @ApiModelProperty("ingress名称，仅在特殊场景会返回")
    private String ingressName;

}
