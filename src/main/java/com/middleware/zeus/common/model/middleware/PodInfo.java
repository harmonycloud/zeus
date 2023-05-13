package com.middleware.zeus.common.model.middleware;

import com.middleware.zeus.common.model.ContainerWithStatus;
import com.middleware.zeus.common.model.MonitorResourceQuota;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Accessors(chain = true)
@Data
@ApiModel("pod信息")
public class PodInfo implements Serializable {

    private static final long serialVersionUID = 9157381680739944630L;

    @ApiModelProperty("pod名称")
    private String podName;
    @ApiModelProperty("所在节点名称")
    private String nodeName;
    @ApiModelProperty("pod ip")
    private String podIp;
    @ApiModelProperty("pod状态")
    private String status;
    @ApiModelProperty("pod角色")
    private String role;
    @ApiModelProperty("创建时间")
    private String createTime;
    @ApiModelProperty("重启次数")
    private Integer restartCount;
    @ApiModelProperty("最近重启时间")
    private String lastRestartTime;
    @ApiModelProperty("pod资源(cpu和内存)")
    private MiddlewareQuota resources;
    @ApiModelProperty("pod存储资源(存储资源)")
    private List<MiddlewareQuota> storageResources;
    @ApiModelProperty("容器列表")
    private List<ContainerWithStatus> containers;
    @ApiModelProperty("初始化容器列表")
    private List<ContainerWithStatus> initContainers;
    @ApiModelProperty("是否已设置备份")
    private Boolean hasConfigBackup;
    @ApiModelProperty("pod绑定的pvc")
    private List<String> pvcs;
    @ApiModelProperty("资源(源自prometheus)")
    private MonitorResourceQuota monitorResourceQuota;
    @ApiModelProperty("节点所在可用区中文名")
    private String nodeZone;
    @ApiModelProperty("可用区编码(zoneA或zoneB)")
    private String zone;
    @ApiModelProperty("pod hostIp")
    private String hostIp;
    @ApiModelProperty("namespace")
    private String namespace;
    @ApiModelProperty("pod分组")
    private String group;
    @ApiModelProperty("pod容器ready信息")
    private String ready;
    @ApiModelProperty("pod容器ready status信息")
    private String readyStatus;
    @ApiModelProperty("pod别名(仅备份恢复会用到)")
    private String podAliasName;
}
