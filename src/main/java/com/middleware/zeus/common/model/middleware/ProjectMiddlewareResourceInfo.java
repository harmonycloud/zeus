package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/4/1 2:18 下午
 */
@Accessors(chain = true)
@Data
@ApiModel("项目下中间件资源信息")
public class ProjectMiddlewareResourceInfo {

    @ApiModelProperty("类型")
    private String type;

    @ApiModelProperty("类型别名")
    private String aliasName;

    @ApiModelProperty("类型别名")
    private String imagePath;

    @ApiModelProperty("资源列表")
    private List<MiddlewareResourceInfo> middlewareResourceInfoList;

}
