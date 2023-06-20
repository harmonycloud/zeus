package com.middleware.zeus.common.model.middleware;

import com.middleware.zeus.common.model.AffinityDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @description kafka专有信息
 * @author  liyinlong
 * @since 2021/10/22 6:57 下午
 */
@Data
@ApiModel("kafka专有信息")
public class KafkaDTO implements Serializable {

    private static final long serialVersionUID = 5636470121720272439L;

    @ApiModelProperty("地址")
    private String zkAddress;

    @ApiModelProperty("端口")
    private String zkPort;

    @ApiModelProperty("后缀路径")
    private String path;

    @ApiModelProperty("是否是自定义")
    private Boolean custom;

    /**
     * broker数量
     */
    private int brokerNum;

    /**
     * 是否开启集群外访问
     */
    private boolean enableExternal;

    @ApiModelProperty("监控采集污点容忍")
    private List<String> exporterTolerations;
    @ApiModelProperty("监控采集节点亲和")
    private List<AffinityDTO> exporterNodeAffinity;
    @ApiModelProperty("manager污点容忍")
    private List<String> managerTolerations;
    @ApiModelProperty("manager节点亲和")
    private List<AffinityDTO> managerNodeAffinity;

}
