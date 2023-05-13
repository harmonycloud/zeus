package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author xutianhong
 * @Date 2022/3/23 2:17 下午
 */
@ApiModel("存储信息")
@Accessors(chain = true)
@Data
public class MiddlewareClusterStorageSupport implements Serializable {

    private static final long serialVersionUID = 56784347219838761L;

    @ApiModelProperty("存储名称")
    private String name;
    @ApiModelProperty("存储类型")
    private String type;
    @ApiModelProperty("分区")
    private String namespace;
}
