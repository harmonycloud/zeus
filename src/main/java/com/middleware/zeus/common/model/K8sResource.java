package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * k8s资源
 * @author liyinlong
 * @since 2023/3/22 3:52 下午
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "中间件资源")
public class K8sResource {

    /**
     * 资源类型 pods ,services mysqlclusters...
     */
    @ApiModelProperty("资源类型 pods ,services mysqlclusters...")
    private String plural;

    /**
     * 资源名称列表
     */
    @ApiModelProperty("资源名称列表")
    private List<String> resourceNameList;

    public K8sResource(String plural, List<String> resourceNameList) {
        this.plural = plural;
        this.resourceNameList = resourceNameList;
    }
}
