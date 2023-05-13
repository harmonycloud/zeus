package com.middleware.zeus.common.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2022/10/13 4:46 下午
 */
@ApiModel("中间件模式对象")
@Accessors(chain = true)
@Data
public class SchemaDto{

    @ApiModelProperty("id")
    private String oid;

    @ApiModelProperty("数据库")
    private String databaseName;

    @ApiModelProperty("模式名称")
    private String schemaName;

    @ApiModelProperty("备注")
    private String comment;

    @ApiModelProperty("owner")
    private String owner;

}
