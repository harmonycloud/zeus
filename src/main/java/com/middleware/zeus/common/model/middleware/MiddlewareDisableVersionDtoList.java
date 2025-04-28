package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2025/4/28 上午10:32
 */
@Data
@NoArgsConstructor
public class MiddlewareDisableVersionDtoList {

    @ApiModelProperty("存储名称")
    private List<MiddlewareDisableVersionDto> middlewareDisableVersionDtoList;

}
