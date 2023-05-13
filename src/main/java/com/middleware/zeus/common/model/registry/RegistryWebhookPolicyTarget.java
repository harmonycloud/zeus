package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2021/01/28
 */
@Accessors(chain = true)
@Data
@ApiModel(value = "制品服务webhook策略目标配置")
public class RegistryWebhookPolicyTarget {

    @ApiModelProperty("http协议")
    private String type;
    @ApiModelProperty("回调地址")
    private String address;
    @ApiModelProperty("是否忽略证书验证")
    private boolean skipCertVerify;
    @ApiModelProperty("授权信息")
    private String authHeader;

}
