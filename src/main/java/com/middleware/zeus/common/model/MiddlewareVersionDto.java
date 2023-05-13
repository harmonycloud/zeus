package com.middleware.zeus.common.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/2/13 3:31 下午
 */
@Data
@NoArgsConstructor
public class MiddlewareVersionDto {

    @ApiModelProperty("主版本号")
    private String masterVersion;

    @ApiModelProperty("子版本号")
    private List<String> slaveVersion;

}
