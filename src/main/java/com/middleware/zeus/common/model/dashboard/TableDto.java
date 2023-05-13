package com.middleware.zeus.common.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/10/13 4:46 下午
 */
@ApiModel("中间件库表对象")
@Accessors(chain = true)
@Data
public class TableDto{

    @ApiModelProperty("id")
    private String oid;

    @ApiModelProperty("数据库")
    private String databaseName;

    @ApiModelProperty("模式名称")
    private String schemaName;

    @ApiModelProperty("table名称")
    private String tableName;

    @ApiModelProperty("表空间")
    private String tablespace;

    @ApiModelProperty("填充率")
    private String fillFactor;

    @ApiModelProperty("字符集")
    private String encoding;

    @ApiModelProperty("校验规则")
    private String collate;

    @ApiModelProperty("owner")
    private String owner;

    @ApiModelProperty("备注")
    private String description;

    @ApiModelProperty("列对象")
    private List<ColumnDto> columnDtoList;

    @ApiModelProperty("外键")
    private List<TableForeignKey> tableForeignKeyList;

    @ApiModelProperty("排它约束")
    private List<TableExclusion> tableExclusionList;

    @ApiModelProperty("唯一约束")
    private List<TableUnique> tableUniqueList;

    @ApiModelProperty("检查约束")
    private List<TableCheck> tableCheckList;

    @ApiModelProperty("继承表")
    private List<TableInherit> tableInheritList;


}
