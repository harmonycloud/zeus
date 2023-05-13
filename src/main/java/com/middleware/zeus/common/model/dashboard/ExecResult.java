package com.middleware.zeus.common.model.dashboard;

import com.alibaba.fastjson.JSONArray;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author liyinlong
 * @since 2022/11/9 3:08 下午
 */
@Data
public class ExecResult {

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("信息")
    private String message;

    @ApiModelProperty("列名数组")
    private JSONArray columns;

    @ApiModelProperty("查询结果数组")
    private JSONArray data;

}
