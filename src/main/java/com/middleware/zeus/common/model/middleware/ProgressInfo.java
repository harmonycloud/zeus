package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/5/8 3:24 下午
 */
@NoArgsConstructor
@Accessors(chain = true)
@Data
@ApiModel("备份恢复进度")
public class ProgressInfo {

    @ApiModelProperty("所属集群")
    private String clusterId;

    @ApiModelProperty("所属分区")
    private String namespace;

    @ApiModelProperty("备份源名称")
    private String backupSourceName;

    @ApiModelProperty("状态")
    private String phrase;

    @ApiModelProperty("存储大小")
    private String storageSize;

    @ApiModelProperty("备份控制器状态（1：正常，0：异常）")
    private Integer backupControllerStatus;

    @ApiModelProperty("当前进度数")
    private Float currentProgress;

    @ApiModelProperty("进度描述")
    private String progressDescription;

    @ApiModelProperty("备份或恢复创建时间")
    private String createTime;

    @ApiModelProperty("备份进程")
    private List<PodInfo> taskPods;

}
