package com.middleware.zeus.common.model.dashboard;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2022/10/21 10:54 下午
 */
@ApiModel("中间件数据库sql执行")
@Accessors(chain = true)
@Data
public class ExecuteSqlDto {

    @ApiModelProperty("id")
    private String id;

    @ApiModelProperty("数据库")
    private String database;

    @ApiModelProperty("执行sql")
    private String sql;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("日期")
    private Date date;

    @ApiModelProperty("耗时")
    private String time;

    @ApiModelProperty("信息")
    private String message;

    @ApiModelProperty("select数据结果")
    private List<Map<String, String>> data;

    @ApiModelProperty("err数据")
    private JSONObject err;
}
