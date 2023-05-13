package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author chwetion
 * @since 2020/12/18 2:22 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "镜像内包含漏洞的包扫描报告")
public class PackageScanReport {
    @ApiModelProperty("包名称")
    private String packageName;
    @ApiModelProperty("包版本")
    private String packageVersion;
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
    @ApiModelProperty("包内漏洞详情")
    private List<LeakDetail> leakDetails;

    public PackageScanReport initStatistics() {
        this.highLeak = 0;
        this.mediumLeak = 0;
        this.lowLeak = 0;
        this.negligibleLeak = 0;
        this.unknownLeak = 0;
        return this;
    }
}
