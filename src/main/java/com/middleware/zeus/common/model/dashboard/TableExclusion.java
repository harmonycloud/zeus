package com.middleware.zeus.common.model.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/10/17 4:20 下午
 */
@ApiModel("中间件库表排它约束")
@Accessors(chain = true)
@Data
public class TableExclusion {

    @ApiModelProperty("id")
    private String oid;

    @ApiModelProperty("外键名称")
    private String name;

    @ApiModelProperty("排它约束")
    private String exclude;

    @ApiModelProperty("排它约束")
    private List<Content> contentList;

    @ApiModelProperty("访问方式")
    private String indexMethod;

    @ApiModelProperty("是否可延迟: DEFERRABLE INITIALLY DEFERRED,DEFERRABLE INITIALLY IMMEDIATE，NOT DEFERRABLE")
    private String deferrablity;

    @ApiModelProperty("操作: delete/add")
    private String operator;

    @Data
    @Accessors(chain = true)
    public static class Content {

        @ApiModelProperty("目标列名")
        private String columnName;

        @ApiModelProperty("排序")
        private String order;

        @ApiModelProperty("操作符")
        private String symbol;
    }

    public String getContent() {
        StringBuilder sb = new StringBuilder();
        for (TableExclusion.Content content : this.getContentList()) {
            sb.append(content.getColumnName()).append(" ").append(content.getOrder()).append(" with ")
                .append(content.getSymbol()).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

}
