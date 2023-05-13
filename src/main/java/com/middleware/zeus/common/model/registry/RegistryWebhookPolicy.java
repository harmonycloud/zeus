package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/01/28
 */
@Accessors(chain = true)
@Data
@ApiModel(value = "制品服务webhook策略")
public class RegistryWebhookPolicy {

    @ApiModelProperty("策略id")
    private Integer id;
    @ApiModelProperty("策略名称")
    private String name;
    @ApiModelProperty("仓库id")
    private String repositoryId;
    @ApiModelProperty("触发事件")
    private List<String> eventTypes;
    @ApiModelProperty("回调目标配置")
    private List<RegistryWebhookPolicyTarget> targets;
    @ApiModelProperty("是否启用")
    private boolean enabled;
    @ApiModelProperty("创建时间")
    private String createTime;
    @ApiModelProperty("更新时间")
    private String updateTime;

}
