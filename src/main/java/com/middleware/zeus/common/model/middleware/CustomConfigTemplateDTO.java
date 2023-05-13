package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/4/25 10:37 上午
 */
@Data
@Accessors(chain = true)
@ApiModel("自定义配置模板DTO")
public class CustomConfigTemplateDTO {

    @ApiModelProperty("模板uid")
    private String uid;

    @ApiModelProperty("模板名称")
    private String name;

    @ApiModelProperty("模板描述")
    private String description;

    @ApiModelProperty("中间件类型")
    private String type;

    @ApiModelProperty("模板字段数量")
    private Integer num;

    @ApiModelProperty("是否重启")
    private Boolean restart;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("模板内容")
    private List<CustomConfig> customConfigList;

    @ApiModelProperty("节点类型")
    private String role;

}
