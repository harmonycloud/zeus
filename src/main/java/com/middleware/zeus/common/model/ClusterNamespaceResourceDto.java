package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/11/8 9:19 上午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "资源池命名空间资源列表")
public class ClusterNamespaceResourceDto extends BaseResourceInfo {


    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区名称")
    private String namespace;

}
