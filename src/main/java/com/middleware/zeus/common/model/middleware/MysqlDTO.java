package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author liyinlong
 * @date 2021/03/23
 */
@Data
@ApiModel("mysql集群信息")
public class MysqlDTO implements Serializable {

    private static final long serialVersionUID = 5636470121720272439L;

    @ApiModelProperty("从节点数量")
    private Integer replicaCount;

    @ApiModelProperty("集群类型")
    private String type;

    @ApiModelProperty("是否打开灾备模式")
    private Boolean openDisasterRecoveryMode;

    @ApiModelProperty("是否是源实例")
    private Boolean isSource;

    @ApiModelProperty("关联集群id")
    private String relationClusterId;

    @ApiModelProperty("关联分区名称")
    private String relationNamespace;

    @ApiModelProperty("mysql中间件名称")
    private String relationName;

    @ApiModelProperty("mysql中间件别称")
    private String relationAliasName;

    @ApiModelProperty("是否已进行过切换")
    private Boolean canSwitch;

    @ApiModelProperty("关联实例是否存在")
    private Boolean relationExist;
    /**
     * Syncing(同步中)\stopSyncing(停止同步)\Error(错误). 每个实例/整体都可以为以上3种状态
     */
    @ApiModelProperty("灾备同步状态")
    private String phase;

    @ApiModelProperty("最近一次同步时间")
    private String lastUpdateTime;

    /**
     * 是否是LVM存储
     */
    private Boolean isLvmStorage;

    /**
     * 是否采集SQL审计日志
     */
    private Boolean auditSqlEnabled;

    /**
     * 是否删除数据库管理相关数据，包括数据库用户信息、数据库信息等
     */
    private Boolean deleteDBManageInfo;

}
