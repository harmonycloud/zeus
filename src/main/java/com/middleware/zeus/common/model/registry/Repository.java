package com.middleware.zeus.common.model.registry;

import com.middleware.zeus.common.model.ProjectDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Accessors(chain = true)
@Data
@ApiModel(description = "镜像仓库")
public class Repository {

    @ApiModelProperty("项目id")
    private String projectId;
    @ApiModelProperty("项目名称")
    private String projectName;
    @ApiModelProperty("租户id")
    private String tenantId;
    @ApiModelProperty("租户名称")
    private String tenantName;
    @ApiModelProperty("仓库名")
    private String name;
    @ApiModelProperty("仓库私有属性，0为共有，1为私有")
    private boolean pvt;
    @ApiModelProperty("仓库在子系统中的仓库编号")
    private String repositoryId;
    @ApiModelProperty("镜像数量")
    private int repoCount;
    @ApiModelProperty("helm chart数量")
    private int chartCount;
    @ApiModelProperty("创建时间")
    private String createTime;
    @ApiModelProperty("配额")
    private ProjectSummaryQuota quota;
    @ApiModelProperty("项目类型，0为镜像，1为chart")
    private int packageType;

    @ApiModelProperty("创建人")
    private String ownerUserId;
    @ApiModelProperty("所属项目")
    private List<ProjectDTO> belongProjects;
    @ApiModelProperty("镜像漏洞统计")
    private ImageScanSummary imageScanSummary;
}
