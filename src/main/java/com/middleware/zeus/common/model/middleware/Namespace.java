package com.middleware.zeus.common.model.middleware;

import com.middleware.zeus.common.model.ContainerIdentityRange;
import com.middleware.zeus.common.model.ResourceQuotaDo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@ApiModel("命名空间")
@Accessors(chain = true)
@Data
public class Namespace {

    @ApiModelProperty("命名空间名称")
    private String name;

    @ApiModelProperty("命名空间别称")
    private String aliasName;

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("集群别名")
    private String clusterAliasName;

    @ApiModelProperty("组织id")
    private String organId;

    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("是否已注册")
    private Boolean registered;

    @ApiModelProperty("中间件实例数")
    private Integer middlewareReplicas;

    @ApiModelProperty("命名空间配额")
    private ResourceQuotaDo quotas;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("状态")
    private String phase;

    @ApiModelProperty("是否开启可用域")
    private boolean availableDomain;

    @ApiModelProperty("uid范围")
    private ContainerIdentityRange containerUIDRange;

    @ApiModelProperty("gid范围")
    private ContainerIdentityRange containerGIDRange;

}
