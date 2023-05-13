package com.middleware.zeus.common.model.middleware;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@ApiModel("制品服务相关")
@Accessors(chain = true)
@Data
public class Registry implements Serializable {

    private static final long serialVersionUID = 965535281836737134L;

    @ApiModelProperty("协议")
    private Integer id;
    @ApiModelProperty("协议")
    private String protocol;
    @ApiModelProperty("域名/IP")
    private String address;
    @ApiModelProperty("端口")
    private Integer port;
    @ApiModelProperty("用户名")
    private String user;
    @ApiModelProperty("密码")
    private String password;
    @ApiModelProperty("版本")
    private String version;
    @ApiModelProperty("类型：harbor/jfrog等")
    private String type;
    @ApiModelProperty("中间件镜像仓库")
    private String imageRepo;
    @ApiModelProperty("helmChart仓库")
    private String chartRepo;

    @JSONField(serialize = false)
    @JsonIgnore
    public String getRegistryAddress() {
        if (StringUtils.isEmpty(address)){
            throw new BusinessException(ErrorMessage.CLUSTER_NOT_ADD_REPOSITORY);
        }
        return this.address + (this.port == null ? "" : ":" + this.port);
    }

    @JSONField(serialize = false)
    @JsonIgnore
    public String getRegistryUrl() {
        return protocol + "://" + getRegistryAddress();
    }

}