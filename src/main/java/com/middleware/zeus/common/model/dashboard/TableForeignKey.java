package com.middleware.zeus.common.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/10/17 4:14 下午
 */
@ApiModel("中间件库表外键信息")
@Accessors(chain = true)
@Data
public class TableForeignKey {

    @ApiModelProperty("id")
    private String oid;

    @ApiModelProperty("外键名称")
    private String name;

    @ApiModelProperty("目标模式")
    private String targetSchema;

    @ApiModelProperty("目标表")
    private String targetTable;

    @ApiModelProperty("目标列名")
    private String columnName;

    @ApiModelProperty("目标列")
    private String targetColumn;

    @ApiModelProperty("外键内容")
    private List<Content> contentList;

    @ApiModelProperty("删除时")
    private String onDelete;

    @ApiModelProperty("更新时")
    private String onUpdate;

    @ApiModelProperty("是否可延迟: DEFERRABLE INITIALLY DEFERRED,DEFERRABLE INITIALLY IMMEDIATE，NOT DEFERRABLE")
    private String deferrablity;

    @ApiModelProperty("操作: delete/add")
    private String operator;

    @Data
    @Accessors(chain = true)
    public static class Content{

        @ApiModelProperty("目标列名")
        private String columnName;

        @ApiModelProperty("目标列")
        private String targetColumn;
    }

    public String getColumn(){
        StringBuilder column = new StringBuilder();
        for (TableForeignKey.Content content : this.getContentList()) {
            column.append(content.getColumnName()).append(",");
        }
        column.deleteCharAt(column.length() - 1);
        return column.toString();
    }

    public String getTarget(){
        StringBuilder target = new StringBuilder();
        for (TableForeignKey.Content content : this.getContentList()) {
            target.append(content.getTargetColumn()).append(",");
        }
        target.deleteCharAt(target.length() - 1);
        return target.toString();
    }

}
