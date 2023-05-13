package com.middleware.zeus.common.model.registry;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2020/12/4 11:44 上午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "制品服务器认证信息")
public class RegistryAuth {
    @ApiModelProperty("制品服务器编号")
    private String registryId;
    @ApiModelProperty("认证字符串")
    private JSONObject authString;
}
