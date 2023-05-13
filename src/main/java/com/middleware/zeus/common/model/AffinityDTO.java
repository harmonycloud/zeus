package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Accessors(chain = true)
@Data
@ApiModel("亲和")
public class AffinityDTO {

    @ApiModelProperty("是否强制亲和")
    private boolean required;

    @ApiModelProperty("标签")
    private String label;

    @ApiModelProperty("命名空间")
    private String namespace;

    @ApiModelProperty("反亲和")
    private Boolean anti;

}
