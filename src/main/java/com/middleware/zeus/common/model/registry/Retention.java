package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chwetion
 * @since 2020/12/14 1:39 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "镜像版本保留策略(每个仓库对应一个策略)")
public class Retention {
    @ApiModelProperty("镜像版本保留策略规则列表")
    private List<RetentionRule> rules = new ArrayList<>();
    @ApiModelProperty("镜像版本保留策略执行任务")
    private RetentionTiming timing;
}
