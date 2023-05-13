package com.middleware.zeus.common.model.middleware;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
@ApiModel("mysql日志查询条件")
public class MiddlewareLogQuery {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区名")
    private String namespace;

    @ApiModelProperty("中间件名称")
    private String middlewareName;

    @ApiModelProperty("中间件类型")
    private String type;

    @ApiModelProperty("开始时间")
    private String startTime;

    @ApiModelProperty("结束时间")
    private String endTime;

    @ApiModelProperty("当前页码")
    private Integer current;

    @ApiModelProperty("每页记录数")
    private Integer size;

    @ApiModelProperty("搜索关键词")
    private String searchWord;

    @ApiModelProperty("搜索类型（match：分词搜索，matchPhrase：精确搜索，wildcard：模糊搜索，regexp：正则表达式搜索,此字段前端不用传）")
    private String searchType;

    @ApiModelProperty("排序规则")
    private String sortOrder;

    private String fromQueryTime;

    private String toQueryTime;

}
