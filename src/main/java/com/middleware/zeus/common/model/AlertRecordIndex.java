package com.middleware.zeus.common.model;

import com.skyview.language.annotations.DirectTranslate;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2023/5/6 1:49 下午
 */
@Data
@Accessors(chain = true)
@ApiModel("告警记录索引")
public class AlertRecordIndex {

    @ApiModelProperty("告警对象名称")
    private String targetName;

    @ApiModelProperty("告警对象别名")
    @DirectTranslate(groupName="alert",uniqueKeyName="targetAliasName", keyName="aliasName")
    private String targetAliasName;

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("集群别名")
    private String clusterAliasName;

    @ApiModelProperty("中间件名称")
    private String middlewareName;

    @ApiModelProperty("中间件类型")
    private String middlewareType;

    @ApiModelProperty("告警记录数量")
    private Integer count;

}
