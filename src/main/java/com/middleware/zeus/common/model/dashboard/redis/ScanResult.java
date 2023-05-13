package com.middleware.zeus.common.model.dashboard.redis;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author liyinlong
 * @since 2022/12/9 11:01 上午
 */
@ApiModel("redis scan结果")
@Accessors(chain = true)
@Data
public class ScanResult {

    @ApiModelProperty("上一次游标(游标值为0表示遍历结束)")
    private Integer preCursor;

    @ApiModelProperty("游标(游标值为0表示遍历结束)")
    private Integer cursor;

    @ApiModelProperty("key列表")
    private List<String> keys;

    @ApiModelProperty("当前scan pod")
    private String pod;

    @ApiModelProperty("上一次scan pod")
    private String prePod;

    @ApiModelProperty("keys总数")
    private Integer sum;


}
