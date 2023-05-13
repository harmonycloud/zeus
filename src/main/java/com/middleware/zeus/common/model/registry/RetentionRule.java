package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2020/12/11 9:55 上午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "镜像版本保留策略规则")
public class RetentionRule {
    @ApiModelProperty("保留规则编号")
    private String id;
    @ApiModelProperty("镜像名选择器")
    private RetentionRuleSelector imageNameSelector;
    @ApiModelProperty("镜像版本选择器")
    private RetentionRuleSelector tagNameSelector;
    @ApiModelProperty("保留策略规则枚举编号(0-全部; 1-最近推送的N; 2-最近拉取的N; 3-最近N天推送; 4-最近N天拉取)")
    private Integer policy;
    @ApiModelProperty("保留策略规则中的数字变量")
    private Integer n;
    @ApiModelProperty("保留策略规则状态(0-禁用; 1-启用)")
    private Integer status;
}
