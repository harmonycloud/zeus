package com.middleware.zeus.common.model.dashboard.mysql;

import com.middleware.zeus.util.ObjectUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * @description 数据列对象
 * @author  liyinlong
 * @since 2022/10/19 5:52 下午
 */
@ApiModel("mysql数据列对象")
@Accessors(chain = true)
@Data
public class ColumnDto {

    @ApiModelProperty("列名称")
    private String column;

    @ApiModelProperty("新列名称")
    private String newColumn;

    @ApiModelProperty("备注")
    private String comment;

    @ApiModelProperty("可空")
    private Boolean nullable;

    @ApiModelProperty("默认值")
    private String columnDefault;

    @ApiModelProperty("类型")
    private String dataType;

    @ApiModelProperty("长度或类型参数")
    private String size;

    @ApiModelProperty("列类型")
    private String columnType;

    @ApiModelProperty("字符集")
    private String charset;

    @ApiModelProperty("校验规则")
    private String collate;

    @ApiModelProperty("是否是主键")
    private boolean primary;

    @ApiModelProperty("是否自增")
    private boolean autoIncrement;

    @ApiModelProperty("列操作类型：1：添加，2：修改(不改列名)，3：修改(修改列名)，4：删除(此字段前端不用传)")
    private int action;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnDto columnDto = (ColumnDto) o;
        return primary == columnDto.primary && autoIncrement == columnDto.autoIncrement && Objects.equals(column, columnDto.column) && ObjectUtil.equals(comment, columnDto.comment) && Objects.equals(nullable, columnDto.nullable) && ObjectUtil.equals(columnDefault, columnDto.columnDefault) && dataType.equalsIgnoreCase(columnDto.getDataType()) && Objects.equals(size, columnDto.size) && ObjectUtil.equals(charset, columnDto.charset) && ObjectUtil.equals(collate, columnDto.collate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, comment, nullable, columnDefault, dataType, size, charset, collate, primary, autoIncrement);
    }
}
