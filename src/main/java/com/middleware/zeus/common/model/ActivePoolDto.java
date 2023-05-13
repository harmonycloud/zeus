package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/5/12 10:14 上午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "可用区")
public class ActivePoolDto {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("资源池名称")
    private String name;

    @ApiModelProperty("可用区列表")
    private List<ActiveAreaDto> areaList;

    @ApiModelProperty("节点列表")
    private List<Node> nodeList;

}
