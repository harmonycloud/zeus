package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author wangpenglei
 * @Date 2022/11/30 下午3:58
 **/
@NoArgsConstructor
@Accessors(chain = true)
@Data
@ApiModel("redis独有字段")
public class RedisParam {

    @ApiModelProperty("主机网络配置")
    private Boolean hostNetwork;

    @ApiModelProperty("代理节点端口")
    private Integer predixyPort;

    @ApiModelProperty("哨兵节点端口")
    private Integer sentinelPort;

    @ApiModelProperty("redis端口")
    private Integer redisPort;

    @ApiModelProperty("exporter端口")
    private Integer exporterPort;

    @ApiModelProperty("predixy节点exporter端口")
    private Integer predixyExporterPort;

}
