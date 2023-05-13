package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

@ApiModel("慢日志")
@Accessors(chain = true)
@Data
@Slf4j
public class MysqlLogDTO {

    @ApiModelProperty("慢日志采集时间")
    private String timestampMysql;

    @ApiModelProperty("sql语句")
    private String query;

    @ApiModelProperty("客户端ip")
    private String clientip;

    @ApiModelProperty("执行时长")
    private String queryTime;

    @ApiModelProperty("锁定时长")
    private String lockTime;

    @ApiModelProperty("解析行数")
    private String rowsExamined;

    @ApiModelProperty("返回行数")
    private String rowsSent;

    @ApiModelProperty("客户端ip")
    private String ip;

    @ApiModelProperty("user")
    private String user;

    @ApiModelProperty("db")
    private String db;

    @ApiModelProperty("queryDate")
    private Date queryDate;

    @ApiModelProperty("sql类型")
    private String queryAction;

    @ApiModelProperty("状态")
    private String status;

    public MysqlLogDTO toDTO(Map map) {
        this.timestampMysql = map.get("@timestamp") == null ? null : String.valueOf(map.get("@timestamp"));
        this.query = map.get("query") == null ? null : String.valueOf(map.get("query"));
        this.clientip = map.get("clientip") == null ? null : String.valueOf(map.get("clientip"));
        this.queryTime = map.get("query_time") == null ? null : String.valueOf(map.get("query_time"));
        this.lockTime = map.get("lock_time") == null ? null : String.valueOf(map.get("lock_time"));
        this.rowsExamined = map.get("rows_examined") == null ? null : String.valueOf(map.get("rows_examined"));
        this.rowsSent = map.get("rows_sent") == null ? null : String.valueOf(map.get("rows_sent"));
        this.ip = map.get("ip") == null ? null : String.valueOf(map.get("ip"));
        this.user = map.get("user") == null ? null : String.valueOf(map.get("user"));
        Object object = map.get("objects");
        if (object != null) {
            try {
                List<Map<String, String>> objects = (List<Map<String, String>>) object;
                if (CollectionUtils.isEmpty(objects)) {
                    return this;
                }
                this.db = objects.get(0).get("db");
            } catch (Exception e) {
                log.error("查询数据库字段出错了");
            }
        } else {
            this.db = map.get("db") == null ? null : String.valueOf(map.get("db"));
        }
        return this;
    }

}
