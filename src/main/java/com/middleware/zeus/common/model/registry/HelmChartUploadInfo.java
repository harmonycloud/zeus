package com.middleware.zeus.common.model.registry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2020/12/16
 */
@ApiModel("helm chart")
@Accessors(chain = true)
@Data
public class HelmChartUploadInfo {

    @ApiModelProperty("名称")
    private String chartName;

    @ApiModelProperty("版本")
    private String chartVersion;

    @ApiModelProperty("最新版本")
    private String registryId;

    @ApiModelProperty("是否过期")
    private String repositoryName;

}
