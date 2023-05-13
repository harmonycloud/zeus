package com.middleware.zeus.common.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2022/10/17 5:14 下午
 */
@ApiModel("中间件库表检查约束")
@Accessors(chain = true)
@Data
public class TableCheck {

    @ApiModelProperty("id")
    private String oid;

    @ApiModelProperty("外键名称")
    private String name;

    @ApiModelProperty("检查内容")
    private String text;

    @ApiModelProperty("是否继承")
    private Boolean noInherit;

    @ApiModelProperty("是否验证")
    private Boolean notValid;

    @ApiModelProperty("操作: delete/add")
    private String operator;

}
