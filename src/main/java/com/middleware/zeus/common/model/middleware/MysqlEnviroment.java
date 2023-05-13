package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * @author yushuaikang
 * @date 2021/10/29 下午3:33
 */
@Data
@Accessors(chain = true)
@ApiModel("自定义mysql环境变量")
public class MysqlEnviroment {

    @ApiModelProperty("参数名")
    private String name;

    @ApiModelProperty("参数值")
    private String value;
}
