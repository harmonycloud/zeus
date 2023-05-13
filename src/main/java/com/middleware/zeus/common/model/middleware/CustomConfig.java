package com.middleware.zeus.common.model.middleware;

import java.util.Date;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/23 4:33 下午
 */
@Data
@Accessors(chain = true)
@ApiModel("自定义配置")
public class CustomConfig {

    @ApiModelProperty("字段")
    private String name;

    @ApiModelProperty("默认值")
    private String defaultValue;

    @ApiModelProperty("当前值")
    private String value;

    @ApiModelProperty("是否重启")
    private Boolean restart;

    @ApiModelProperty("阈值")
    private String ranges;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("参数类型：select,input")
    private String paramType;

    @ApiModelProperty("正则校验")
    private String pattern;

    @ApiModelProperty("修改时间")
    private Date updateTime;

    @ApiModelProperty("是否置顶")
    private Boolean topping;

    @ApiModelProperty("节点类型")
    private String role;
}
