package com.middleware.zeus.common.model.registry;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
@ApiModel(description = "制品服务器")
public class Registry {
    @ApiModelProperty("制品服务器编号")
    private String id;
    @ApiModelProperty("制品服务器所属数据中心编号")
    private String dcId;
    @ApiModelProperty("制品服务器名称")
    private String name;
    @ApiModelProperty("制品服务器别名/显示名称")
    private String nickname;
    @ApiModelProperty("制品服务器类型，目前支持的类型有{'harbor', 'jfrog'}")
    private String type;
    @ApiModelProperty("制品服务器主版本号")
    private int majorVersion;
    @ApiModelProperty("制品服务器次版本号")
    private int minorVersion;
    @ApiModelProperty("制品服务器访问协议，{'http', 'https'}")
    private String protocol;
    @ApiModelProperty("制品服务器访问域名/IP")
    private String host;
    @ApiModelProperty("制品服务器访问端口")
    private int port;
    @ApiModelProperty("制品服务器属性，不同制品服务器属性不同" +
            "harbor类型：{\"username\":\"账号\",\"password\":\"密码\"}")
    private JSONObject attributes;
    @ApiModelProperty("制品服务状态")
    private boolean normal;
    @ApiModelProperty("制品服务地址，返回结果使用，如：10.10.101.176:8443")
    private String address;
    @ApiModelProperty("制品服务url，返回结果使用，如：https://10.10.101.176:8443")
    private String url;
    @ApiModelProperty("制品服务添加时间，格式为yyyy-MM-dd HH:mm:ss")
    private String createTime;

}