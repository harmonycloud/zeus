package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2020/12/14 9:49 上午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "镜像版本保留策略规则选择器")
public class RetentionRuleSelector {
    @ApiModelProperty("选择器操作符(harbor中支持'match'和'exclude'两种操作)")
    private String op;
    @ApiModelProperty("选择器操作对象表达式")
    private String exp;
}
