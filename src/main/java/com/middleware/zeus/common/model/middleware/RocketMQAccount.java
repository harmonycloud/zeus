package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/8/10 7:15 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("rocketMQ ACL认证 账号信息配置")
public class RocketMQAccount {

    @ApiModelProperty("账号")
    private String accessKey;
    @ApiModelProperty("密码")
    private String secretKey;
    @ApiModelProperty("是否admin")
    private Boolean admin;
    @ApiModelProperty("用户白名单")
    private String whiteRemoteAddress;
    @ApiModelProperty("topic权限")
    private Map<String, String> topicPerms;
    @ApiModelProperty("消费组权限")
    private Map<String, String> groupPerms;

}
