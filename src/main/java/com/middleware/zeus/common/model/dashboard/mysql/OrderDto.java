package com.middleware.zeus.common.model.dashboard.mysql;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2022/10/24 2:02 下午
 */
@ApiModel("排序规则")
@Accessors(chain = true)
@Data
public class OrderDto {

    @ApiModelProperty("列名")
    private String column;

    @ApiModelProperty("排序方式（正序：asc，倒序：desc）")
    private String order;

}
