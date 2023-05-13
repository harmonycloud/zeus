package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author chwetion
 * @since 2020/12/17 5:09 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "镜像特定版本扫描报告")
public class TagScanReport {
    @ApiModelProperty("严重漏洞")
    private Integer criticalLeak;
    @ApiModelProperty("高危漏洞")
    private Integer highLeak;
    @ApiModelProperty("中危漏洞")
    private Integer mediumLeak;
    @ApiModelProperty("低危漏洞")
    private Integer lowLeak;
    @ApiModelProperty("可忽略漏洞")
    private Integer negligibleLeak;
    @ApiModelProperty("未知漏洞")
    private Integer unknownLeak;
    @ApiModelProperty("镜像内包含漏洞的包扫描报告")
    private List<PackageScanReport> packages;
}
