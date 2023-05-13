package com.middleware.zeus.common.model.dashboard.mysql;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

/**
 * mysql索引
 * @author liyinlong
 * @since 2022/10/18 8:51 下午
 */
@ApiModel("mysql索引对象")
@Accessors(chain = true)
@Data
public class IndexDto {

    @ApiModelProperty("索引名称")
    private String index;

    @ApiModelProperty("索引类型(primary、unique)")
    private String type;

    @ApiModelProperty("存储类型(BTREE)")
    private String storageType;

    @ApiModelProperty("备注")
    private String comment;

    @ApiModelProperty("关联列")
    private List<IndexColumnDto> indexColumns;

    @ApiModelProperty("索引操作类型：1：添加，2：修改，4：删除(此字段前端不用传)")
    private int action;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexDto indexDto = (IndexDto) o;
        return Objects.equals(index, indexDto.index) && Objects.equals(type, indexDto.type) && Objects.equals(storageType, indexDto.storageType) && Objects.equals(indexColumns, indexDto.indexColumns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, type, storageType, indexColumns);
    }

}
