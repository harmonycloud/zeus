package com.middleware.zeus.common.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author LanChao
 * @date 2021-01-16 07:00:23
 */
@Accessors(chain = true)
@Data
@JsonTypeName("openyurt")
public class OpenYurtInternalService extends InternalService {
    @ApiModelProperty("边缘节点组名称")
    private String edgeNodeName;
}
