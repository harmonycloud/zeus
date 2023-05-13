package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author wangpenglei
 * @Date 2022/11/29 下午8:21
 **/
@NoArgsConstructor
@Accessors(chain = true)
@Data
@ApiModel("postgresql独有字段")
public class PostgresqlParam {

    @ApiModelProperty("主机网络配置")
    private Boolean hostNetwork;

    @ApiModelProperty("pg端口")
    private Integer pgPort;

    @ApiModelProperty("patroni端口")
    private Integer apiPort;

    @ApiModelProperty("exporter端口")
    private Integer exporterPort;

    @ApiModelProperty("进程监控端口")
    private Integer bgMonPort;

}
