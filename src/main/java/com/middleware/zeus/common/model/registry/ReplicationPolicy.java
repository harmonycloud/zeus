package com.middleware.zeus.common.model.registry;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2020/12/20 2:34 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "镜像复制策略")
public class ReplicationPolicy {
    @ApiModelProperty("策略编号")
    private String id;
    @ApiModelProperty("策略名称")
    private String ruleName;
    @ApiModelProperty("策略描述")
    private String description;
    @ApiModelProperty("源仓库")
    private String repository;
    @ApiModelProperty("复制规则镜像筛选器")
    private ReplicationImageSelector selector;
    @ApiModelProperty("目标制品库")
    private ReplicationTargetRegistry targetRegistry;
    @ApiModelProperty("目标仓库名")
    private String targetRepository;
    @ApiModelProperty("复制方式，(pull-从目标向本服务器复制；push-从本服务器向目标复制)")
    private String syncType;
    @ApiModelProperty("策略模式，(manual-手动；scheduled-定时)")
    private String mode;
    @ApiModelProperty("同步时是否覆盖同名镜像")
    private Boolean override;
    @ApiModelProperty("策略创建时间")
    private String createTime;
    @ApiModelProperty("模式设置")
    private JSONObject modeSetting;
}
