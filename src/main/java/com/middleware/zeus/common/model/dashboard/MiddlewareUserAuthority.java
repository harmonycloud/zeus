package com.middleware.zeus.common.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2022/10/10 3:53 下午
 */
@ApiModel("中间件用户权限")
@Accessors(chain = true)
@Data
public class MiddlewareUserAuthority {

    @ApiModelProperty("数据库")
    private String database;

    @ApiModelProperty("模式")
    private String schema;

    @ApiModelProperty("数据表")
    private String table;

    @ApiModelProperty("权限")
    private String authority;

    @ApiModelProperty("可分配")
    private Boolean grantAble;

}
