package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author xutianhong
 * @Date 2021/4/28 4:03 下午
 */
@Data
@Accessors(chain = true)
@ApiModel("自定义配置历史DTO")
public class CustomConfigHistoryDTO {

    @ApiModelProperty("id")
    private Integer id;

    @ApiModelProperty("中间件名称")
    private String name;

    @ApiModelProperty("字段名")
    private String item;

    @ApiModelProperty("修改前")
    private String last;

    @ApiModelProperty("修改后")
    private String after;

    @ApiModelProperty("修改日期")
    private Date date;

    @ApiModelProperty("是否重启")
    private Boolean restart;

    @ApiModelProperty("是否生效")
    private Boolean status;

    @ApiModelProperty("节点类型")
    private String role;
}
