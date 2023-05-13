package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author dengyulong
 * @date 2020/12/03
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Data
@ApiModel(description = "制品服务详情")
public class RegistryDetail extends Registry {

    @ApiModelProperty("统计信息")
    private Statistic statistics;

    @ApiModelProperty("配置信息")
    private Configuration configurations;

    @ApiModelProperty("配额信息")
    private ProjectSummaryQuota quota;

    @ApiModelProperty("仓库列表")
    private List<Repository> repositories;

}
