package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/12/24 10:04 上午
 */
@Data
@Accessors(chain = true)
@ApiModel(description = "prometheus告警")
public class YamlCheck {

    @ApiModelProperty("校验结果")
    private Boolean flag;

    @ApiModelProperty("错误信息")
    private List<String> msgList;

}
