package com.middleware.zeus.common.model.dashboard.mysql;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @description
 * @author  liyinlong
 * @since 2022/10/19 5:55 下午
 */
@ApiModel("mysql数据表对象")
@Accessors(chain = true)
@Data
public class TableDto{

    @ApiModelProperty("table名称")
    private String tableName;

    @ApiModelProperty("新table名称")
    private String newTableName;

    @ApiModelProperty("字符集")
    private String charset;

    @ApiModelProperty("校验规则")
    private String collate;

    @ApiModelProperty("行数")
    private int rows;

    @ApiModelProperty("自增值")
    private Integer autoIncrement;

    @ApiModelProperty("最小行")
    private Integer minRows;

    @ApiModelProperty("最大行")
    private Integer maxRows;

    @ApiModelProperty("备注")
    private String comment;

    @ApiModelProperty("行格式")
    private String rowFormat;

    @ApiModelProperty("存储引擎")
    private String engine;

    @ApiModelProperty("列信息")
    private List<ColumnDto> columns;

    @ApiModelProperty("索引信息")
    private List<IndexDto> indices;

    @ApiModelProperty("外键信息")
    private List<ForeignKeyDto> foreignKeys;

    /**
     * 主键操作类型:：1：添加，2：修改，4：删除，5：不做改动(此字段前端不用传)
     */
    private int primaryKeyAction;

}
