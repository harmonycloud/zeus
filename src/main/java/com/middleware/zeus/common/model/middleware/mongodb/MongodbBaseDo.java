package com.middleware.zeus.common.model.middleware.mongodb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2025/3/7 1:04 AM
 */
@Accessors(chain = true)
@Data
@ApiModel("Mongodb基础信息")
@AllArgsConstructor
@NoArgsConstructor
public class MongodbBaseDo {

    @ApiModelProperty("协议")
    private String protocol;

    @ApiModelProperty("地址")
    private String path;

    @ApiModelProperty("端口")
    private String port;

    @ApiModelProperty("公钥")
    private String publicKey;

    @ApiModelProperty("私钥")
    private String privateKey;

}
