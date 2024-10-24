package com.middleware.zeus.common.model;

import com.skyview.language.annotations.DirectTranslate;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/5/12 9:58 上午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "可用区")
public class ActiveAreaDto {

    @ApiModelProperty("可用区名称")
    private String name;

    @ApiModelProperty("可用区别名")
    @DirectTranslate(groupName="active_area",uniqueKeyName="aliasName", keyName="aliasName")
    private String aliasName;

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("是否已经初始化")
    private Boolean init;

    @ApiModelProperty("节点数量")
    private Integer node;

    @ApiModelProperty("正常节点数量")
    private Integer runningNodeCount;

    @ApiModelProperty("异常节点数量")
    private Integer errorNodeCount;

    @ApiModelProperty("cpu使用量")
    private Double cpuUsed;

    @ApiModelProperty("cpu总量")
    private Double cpuTotal;

    @ApiModelProperty("cpu使用率")
    private Double cpuRate;

    @ApiModelProperty("memory、使用量")
    private Double memoryUsed;

    @ApiModelProperty("memory总量")
    private Double memoryTotal;

    @ApiModelProperty("memory使用率")
    private Double memoryRate;

    @ApiModelProperty("节点列表")
    private List<Node> nodeList;

}
