package com.middleware.zeus.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
@JsonInclude(value=JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServicePort {
    @ApiModelProperty("协议")
    private String protocol;
    @ApiModelProperty("端口名称")
    private String name;
    @ApiModelProperty("端口号")
    private Integer port;
    @ApiModelProperty("映射端口")
    private Integer targetPort;
    @ApiModelProperty("主机端口(服务类型为NodePort时填写)")
    private Integer nodePort;
}
