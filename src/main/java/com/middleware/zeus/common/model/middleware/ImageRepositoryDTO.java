package com.middleware.zeus.common.model.middleware;

import java.util.Date;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author yushuaikang
 * @date 2022/3/10 下午2:32
 */
@Accessors(chain = true)
@Data
@ApiModel("镜像仓库")
public class ImageRepositoryDTO {

    @ApiModelProperty("id")
    private Integer id;

    @ApiModelProperty("集群ID")
    private String clusterId;

    @ApiModelProperty("协议")
    private String protocol;

    @ApiModelProperty("harbor地址")
    private String address;

    @ApiModelProperty("harbor主机地址")
    private String hostAddress;

    @ApiModelProperty("harbor项目")
    private String project;

    @ApiModelProperty("端口号")
    private Integer port;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("是否默认")
    private Integer isDefault;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("更新时间")
    private Date updateTime;

    @ApiModelProperty("version")
    private String version;

    public String getRegistryAddress() {
        return this.hostAddress + (this.port == null ? "" : ":" + this.port);
    }
}
