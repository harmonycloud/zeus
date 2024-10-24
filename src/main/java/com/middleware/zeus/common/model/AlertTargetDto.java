package com.middleware.zeus.common.model;

import com.skyview.language.annotations.Translate;
import com.skyview.language.annotations.TranslateGroupInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2023/5/6 3:49 下午
 */
@Data
@Accessors(chain = true)
@ApiModel("告警对象数据结构")
@TranslateGroupInfo(name="alert_target",uniqueKeyName="name")
public class AlertTargetDto {

    @ApiModelProperty("告警对象名称")
    private String name;

    @ApiModelProperty("告警对象别名")
    @Translate(keyName="alias_name")
    private String aliasName;

    @ApiModelProperty("告警类型: system/cluster/service")
    private String alertType;

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区")
    private String namespace;

    @ApiModelProperty("告警规则文件名称")
    private String prometheusRuleName;

    @ApiModelProperty("是否存在")
    private Boolean exist;

}
