package com.middleware.zeus.common.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2022/10/19 3:33 下午
 */
@ApiModel("中间件库表继承")
@Accessors(chain = true)
@Data
public class TableInherit {

    @ApiModelProperty("id")
    private String oid;

    @ApiModelProperty("数据库")
    private String databaseName;

    @ApiModelProperty("模式名称")
    private String schemaName;

    @ApiModelProperty("table名称")
    private String tableName;

    @ApiModelProperty("操作: delete/add")
    private String operator;

    public Boolean equals(TableInherit inherit) {
        return this.getDatabaseName().equals(inherit.getDatabaseName())
            && this.getSchemaName().equals(inherit.getSchemaName())
            && this.getTableName().equals(inherit.getTableName());
    }
}
