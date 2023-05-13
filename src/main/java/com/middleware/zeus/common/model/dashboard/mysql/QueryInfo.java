package com.middleware.zeus.common.model.dashboard.mysql;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author liyinlong
 * @since 2022/10/24 10:42 上午
 */
@ApiModel("数据表查询信息对象")
@Accessors(chain = true)
@Data
public class QueryInfo {

    @ApiModelProperty("页码")
    private Integer index;

    @ApiModelProperty("偏移量")
    private Integer offset;

    @ApiModelProperty("每页数量")
    private Integer pageSize;

    @ApiModelProperty("关键词")
    private String keyword;

    @ApiModelProperty("排序规则列表")
    private List<OrderDto> orderDtoList;

}
