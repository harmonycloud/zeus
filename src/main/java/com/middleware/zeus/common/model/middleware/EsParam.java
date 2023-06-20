package com.middleware.zeus.common.model.middleware;

import com.middleware.zeus.common.model.AffinityDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @auther wangpenglei
 * @date 2023/3/14 11:30
 */
@NoArgsConstructor
@Accessors(chain = true)
@Data
@ApiModel("es独有字段")
public class EsParam {

    @ApiModelProperty("主机网络配置")
    private Boolean hostNetwork;

    @ApiModelProperty("export节点端口")
    private Integer exporterPort;

    @ApiModelProperty("master节点http端口")
    private Integer httpPort;

    @ApiModelProperty("kibana节点端口")
    private Integer kibanaPort;

    @ApiModelProperty("master节点tcp端口")
    private Integer tcpPort;

    @ApiModelProperty("监控采集污点容忍")
    private List<String> exporterTolerations;
    @ApiModelProperty("监控采集节点亲和")
    private List<AffinityDTO> exporterNodeAffinity;
    @ApiModelProperty("kibana污点容忍")
    private List<String> kibanaTolerations;
    @ApiModelProperty("kibana节点亲和")
    private List<AffinityDTO> kibanaNodeAffinity;
}
