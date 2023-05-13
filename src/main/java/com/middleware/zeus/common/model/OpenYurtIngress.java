package com.middleware.zeus.common.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author chwetion
 * @since 2021/1/15 9:59 上午
 */
@JsonTypeName("openyurt")
@Data
public class OpenYurtIngress extends Ingress {
    @ApiModelProperty("边缘节点组名称")
    private String edgeNodeName;
}
