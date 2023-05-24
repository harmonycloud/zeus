package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author dengyulong
 * @date 2021/03/30
 */
@Accessors(chain = true)
@Data
@ApiModel("集群证书信息")
public class ClusterCert implements Serializable {

    private static final long serialVersionUID = 3661972519735453048L;

    @ApiModelProperty("认证信息，admin.conf全部内容，由下面三个字段组装")
    private String certificate;

    @ApiModelProperty("服务端证书")
    private String certificateAuthorityData;
    @ApiModelProperty("用户的客户端证书")
    private String clientCertificateData;
    @ApiModelProperty("用户的客户端key")
    private String clientKeyData;

    // token方式添加集群时使用
    @ApiModelProperty("accessToken")
    private String accessToken;

}
