package com.middleware.zeus.common.model.dashboard.mysql;

import com.middleware.zeus.util.ObjectUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

/**
 * @author liyinlong
 * @since 2022/10/20 12:39 下午
 */
@ApiModel("外键对象")
@Accessors(chain = true)
@Data
public class ForeignKeyDto {

    @ApiModelProperty("外键名称")
    private String foreignKey;

    @ApiModelProperty("参考库")
    private String referenceDatabase;

    @ApiModelProperty("参考表")
    private String referenceTable;

    @ApiModelProperty("删除时执行操作")
    private String onDeleteOption;

    @ApiModelProperty("更新时执行操作")
    private String onUpdateOption;

    @ApiModelProperty("外键列详情")
    private List<ForeignKeyDetailDto> details;

    @ApiModelProperty("外键操作类型：1：添加，2：修改，4：删除(此字段前端不用传)")
    private int action;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForeignKeyDto that = (ForeignKeyDto) o;
        return Objects.equals(foreignKey, that.foreignKey) && Objects.equals(referenceDatabase, that.referenceDatabase) && Objects.equals(referenceTable, that.referenceTable) && ObjectUtil.equals(onDeleteOption, that.onDeleteOption) && ObjectUtil.equals(onUpdateOption, that.onUpdateOption) && Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(foreignKey, referenceDatabase, referenceTable, onDeleteOption, onUpdateOption, details);
    }
}
