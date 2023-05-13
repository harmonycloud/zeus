package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author chwetion
 * @since 2020/12/4 2:58 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "制品服务器仓库标签")
public class Label {
    @ApiModelProperty("标签编号")
    private String id;
    @ApiModelProperty("标签类型，'harbor_global'-全局标签; 'harbor_repository'-仓库标签")
    private String type;
    @ApiModelProperty("标签名")
    private String name;
    @ApiModelProperty("标签描述")
    private List<String> description;
    @ApiModelProperty("标签创建时间")
    private String creationTime;
}
