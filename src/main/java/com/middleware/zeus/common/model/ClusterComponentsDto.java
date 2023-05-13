package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author anson
 * @since 2021/1/11 11:16
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "集群组件")
public class ClusterComponentsDto {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("集群别名")
    private String clusterAliasName;

    @ApiModelProperty("组件名称")
    private String component;

    @ApiModelProperty("部署模式: simple,high")
    private String type;

    @ApiModelProperty("协议:http,https")
    private String protocol;

    @ApiModelProperty("主机地址")
    private String host;

    @ApiModelProperty("端口")
    private String port;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("状态：0-未安装接入 1-已接入 2-安装中 3-运行正常 4-运行异常 5-卸载中")
    private Integer status;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("创建后经过时间")
    private long seconds;

    // 下为组件安装表单用

    /**
     * 监控告警组件
     */
    @ApiModelProperty("全局静默时间")
    private String silentTime;

    /**
     * lvm组件
     */
    @ApiModelProperty("vg名称")
    private String vgName;

    @ApiModelProperty("存储限额")
    private String size;

    /**
     * 日志组件
     */
    @ApiModelProperty("日志采集组件")
    private Boolean logCollect;

    @ApiModelProperty("日志保留时间")
    private String logSaveTime;

    /**
     * alertmanager组件
     */
    @ApiModelProperty("平台地址协议")
    private String platformProtocol;
    @ApiModelProperty("平台地址ip")
    private String platformHost;
    @ApiModelProperty("平台地址端口")
    private Integer platformPort;

    public String getAddress() {
        return this.protocol + "://" + this.host + (this.port == null ? "" : ":" + this.port);
    }



}
