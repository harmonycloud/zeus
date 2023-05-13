package com.middleware.zeus.common.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2022/10/14 9:26 上午
 */
@ApiModel("中间件列对象")
@Accessors(chain = true)
@Data
public class ColumnDto {

    @ApiModelProperty("id")
    private String oid;

    @ApiModelProperty("列序号")
    private String num;

    @ApiModelProperty("数据库")
    private String databaseName;

    @ApiModelProperty("模式名称")
    private String schemaName;

    @ApiModelProperty("table名称")
    private String tableName;

    @ApiModelProperty("列名称")
    private String column;

    @ApiModelProperty("备注")
    private String comment;

    @ApiModelProperty("可空")
    private Boolean nullable;

    @ApiModelProperty("自增")
    private Boolean inc;

    @ApiModelProperty("默认值")
    private String defaultValue;

    @ApiModelProperty("是否主键")
    private Boolean primaryKey;

    @ApiModelProperty("数组")
    private Boolean array;

    @ApiModelProperty("数据类型")
    private String dataType;

    @ApiModelProperty("长度")
    private String size;

    @ApiModelProperty("字符集")
    private String encoding;

    @ApiModelProperty("校验规则")
    private String collate;

    @ApiModelProperty("owner")
    private String owner;

}
