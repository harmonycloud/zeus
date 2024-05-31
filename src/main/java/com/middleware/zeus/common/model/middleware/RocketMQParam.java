package com.middleware.zeus.common.model.middleware;

import com.middleware.zeus.common.model.AffinityDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/8/10 7:10 下午
 */
@NoArgsConstructor
@Accessors(chain = true)
@Data
@ApiModel("rocketMQ独有字段")
public class RocketMQParam {

    @ApiModelProperty("rocketMQ ACL认证")
    public RocketMQACL acl;

    @ApiModelProperty("自定义集群实例数量")
    public Integer replicas;

    @ApiModelProperty("Dleaer组数")
    public Integer group;

    /**
     * broker数量
     */
    private int brokerNum;

    /**
     * proxy数量
     */
    private int proxyNum;

    /**
     * 是否开启集群外访问
     */
    private boolean enableExternal;

    @ApiModelProperty("是否自动创建Topic")
    private Boolean autoCreateTopicEnable;

    @ApiModelProperty("监控采集污点容忍")
    private List<String> exporterTolerations;
    @ApiModelProperty("监控采集节点亲和")
    private List<AffinityDTO> exporterNodeAffinity;
    @ApiModelProperty("console污点容忍")
    private List<String> consoleTolerations;
    @ApiModelProperty("console节点亲和")
    private List<AffinityDTO> consoleNodeAffinity;

}
