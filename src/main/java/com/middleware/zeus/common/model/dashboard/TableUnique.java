package com.middleware.zeus.common.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2022/10/17 5:13 下午
 */
@ApiModel("中间件库表唯一约束")
@Accessors(chain = true)
@Data
public class TableUnique {

    @ApiModelProperty("id")
    private String oid;

    @ApiModelProperty("外键名称")
    private String name;

    @ApiModelProperty("目标列名")
    private String columnName;

    @ApiModelProperty("是否可延迟: DEFERRABLE INITIALLY DEFERRED,DEFERRABLE INITIALLY IMMEDIATE，NOT DEFERRABLE")
    private String deferrablity;

    @ApiModelProperty("操作: delete/add")
    private String operator;

}
