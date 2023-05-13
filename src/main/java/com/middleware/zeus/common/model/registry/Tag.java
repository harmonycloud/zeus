package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author chwetion
 * @since 2020/12/16 5:31 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "镜像版本")
public class Tag {
    @ApiModelProperty("版本名")
    private String name;
    @ApiModelProperty("创建时间")
    private String createTime;
    @ApiModelProperty("最后一次推送时间")
    private String pushTime;
    @ApiModelProperty("镜像版本大小(单位: k)")
    private Long size;
    @ApiModelProperty("版本扫描报告")
    private TagScanReportOverride report;
    @ApiModelProperty("本地拉取状态, 0-未拉取; 1-正在拉取; 2-已拉取")
    private int status;
    @ApiModelProperty("版本拥有的标签")
    private List<Label> labels;
    @ApiModelProperty("摘要")
    private String digest;
    @ApiModelProperty("构建历史(dockerfile)")
    private List<ImageBuildHistory> imageBuildHistory;
}
