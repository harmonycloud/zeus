package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author chwetion
 * @since 2021/1/18 5:57 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("版本扫描报告简略信息")
public class TagScanReportOverride {
    @ApiModelProperty("安全状态(0-未扫描; 1-安全镜像; 2-存在漏洞)")
    private Integer status;
    @ApiModelProperty("当安全等级为存在漏洞时，该值为漏洞总计个数")
    private Integer n;
    @ApiModelProperty("安全级别")
    private String severity;
}
