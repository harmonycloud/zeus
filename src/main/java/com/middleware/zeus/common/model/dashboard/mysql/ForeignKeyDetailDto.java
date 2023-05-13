package com.middleware.zeus.common.model.dashboard.mysql;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * @author liyinlong
 * @since 2022/10/20 12:42 下午
 */
@ApiModel("外键详情")
@Accessors(chain = true)
@Data
public class ForeignKeyDetailDto {

    @ApiModelProperty("所属外键名称")
    private String foreignKey;

    @ApiModelProperty("参考库")
    private String referenceDatabase;

    @ApiModelProperty("参考表")
    private String referenceTable;

    @ApiModelProperty("列名称")
    private String column;

    @ApiModelProperty("参考列名称")
    private String referencedColumn;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForeignKeyDetailDto that = (ForeignKeyDetailDto) o;
        return Objects.equals(column, that.column) && Objects.equals(referencedColumn, that.referencedColumn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, referencedColumn);
    }
}
