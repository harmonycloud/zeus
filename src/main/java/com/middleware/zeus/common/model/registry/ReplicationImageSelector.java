package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2020/12/20 2:45 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "镜像复制策略镜像筛选器")
public class ReplicationImageSelector {
    @ApiModelProperty("匹配镜像名称")
    private String imagePattern;
    @ApiModelProperty("匹配镜像版本")
    private String tagPattern;
}
