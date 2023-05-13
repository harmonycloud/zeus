package com.middleware.zeus.common.model.registry;

import java.util.List;
import java.util.Map;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author dengyulong
 * @date 2020/12/16
 */
@ApiModel("helm chart版本详情")
@Accessors(chain = true)
@Data
public class HelmChartVersionDetail {

    @ApiModelProperty("元数据，即版本数据")
    private HelmChartVersion metadata;

    @ApiModelProperty("标签")
    private List<Object> labels;

    @ApiModelProperty("安全信息")
    private Map<String, Object> security;

    @ApiModelProperty("依赖")
    private List<Object> dependencies;

    @ApiModelProperty("文件")
    private Map<String, String> files;

    @ApiModelProperty("参数值")
    private Map<String, Object> values;

    @ApiModelProperty("yaml文件")
    private Map<String, String> yamlFileMap;

    private String fileIndex;

    private String tarFileName;

}
