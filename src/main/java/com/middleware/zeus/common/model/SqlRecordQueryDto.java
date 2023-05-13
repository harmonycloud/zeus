package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author liyinlong
 * @since 2022/11/12 10:52 上午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "SQL执行记录查询条件")
public class SqlRecordQueryDto {

    @ApiModelProperty("关键词")
    private String keyword;

    @ApiModelProperty("开始时间(例：2022-08-08 12:22:22 )")
    private String startTime;

    @ApiModelProperty("结束时间")
    private String endTime;

    @ApiModelProperty("页码")
    private Integer pageNum;

    @ApiModelProperty("没页记录数")
    private Integer size;

    @ApiModelProperty("状态(true:成功，false:失败，null：全部)")
    private Boolean execStatus;

    @ApiModelProperty("执行时间排序方式(true：正序，false：倒序，null：忽略该字段)")
    private Boolean ascExecDateOrder;

    @ApiModelProperty("耗时排序(true：正序，false：倒序，null：忽略该字段)")
    private Boolean ascExecTimeOrder;

}
