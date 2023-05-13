package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2021/01/21
 */
@ApiModel(description = "镜像扫描统计")
@Accessors(chain = true)
@Data
public class ImageScanSummary {

    @ApiModelProperty("未扫描镜像数")
    private int notScanned;

    @ApiModelProperty("安全镜像数")
    private int secure;

    @ApiModelProperty("中低危险镜像数")
    private int mediumLow;

    @ApiModelProperty("高风险镜像数")
    private int highCritical;

    @ApiModelProperty("不支持镜像数")
    private int unsupported;

}
