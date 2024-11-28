package com.middleware.zeus.common.model.middleware;

import com.middleware.zeus.common.model.BaseResourceInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/11/3 11:27 上午
 */
@Accessors(chain = true)
@NoArgsConstructor
@Data
@ApiModel("中间件资源信息")
public class MiddlewareResourceInfo extends BaseResourceInfo {

    @ApiModelProperty("集群id")
    private String clusterId;

    @ApiModelProperty("分区")
    private String namespace;

    @ApiModelProperty("中间件名称")
    private String name;

    @ApiModelProperty("中间件中文别名")
    private String aliasName;

    @ApiModelProperty("中间件类型")
    private String type;

    @ApiModelProperty("中间件版本")
    private String chartVersion;

    @ApiModelProperty("图片地址")
    private String imagePath;

    public MiddlewareResourceInfo(String clusterId, String namespace, String name, String type) {
        this.clusterId = clusterId;
        this.namespace = namespace;
        this.name = name;
        this.type = type;
    }

}
