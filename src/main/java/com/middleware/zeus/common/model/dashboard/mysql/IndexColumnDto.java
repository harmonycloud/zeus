package com.middleware.zeus.common.model.dashboard.mysql;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * @author liyinlong
 * @since 2022/10/18 9:00 下午
 */
@ApiModel("mysql索引列对象")
@Accessors(chain = true)
@Data
public class IndexColumnDto {

    @ApiModelProperty("索引名称")
    private String keyName;

    @ApiModelProperty("索引类型")
    private String indexType;

    @ApiModelProperty("列名称")
    private String columnName;

    @ApiModelProperty("长度")
    private Integer subPart;

    @ApiModelProperty("索引备注")
    private String indexComment;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexColumnDto that = (IndexColumnDto) o;
        return Objects.equals(columnName, that.columnName) && Objects.equals(subPart, that.subPart);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnName, subPart);
    }
}
