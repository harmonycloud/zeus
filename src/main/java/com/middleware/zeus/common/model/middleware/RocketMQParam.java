package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

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
     * 是否开启集群外访问
     */
    private boolean enableExternal;

    @ApiModelProperty("是否自动创建Topic")
    private Boolean autoCreateTopicEnable;

}
