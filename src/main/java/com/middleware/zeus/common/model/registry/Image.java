package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
@ApiModel(description = "镜像")
public class Image {
    @ApiModelProperty("镜像名")
    private String name;
    @ApiModelProperty("带仓库地址的镜像名称")
    private String nameWithRegistryAddr;
    @ApiModelProperty("版本数量")
    private int tagCount;
    @ApiModelProperty("最新更新的tag")
    private Tag newest;
    @ApiModelProperty("所属仓库")
    private Repository repository;
}
