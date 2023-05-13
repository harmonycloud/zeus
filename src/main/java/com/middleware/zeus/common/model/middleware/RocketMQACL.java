package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/8/10 7:13 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("rocketMQ ACL认证")
public class RocketMQACL {

    @ApiModelProperty("是否开启")
    private Boolean enable;

    @ApiModelProperty("全局白名单")
    private String globalWhiteRemoteAddresses;

    @ApiModelProperty("账号信息配置")
    private List<RocketMQAccount> rocketMQAccountList;

}
