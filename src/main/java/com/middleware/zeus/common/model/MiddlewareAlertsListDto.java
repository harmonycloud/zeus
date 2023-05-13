package com.middleware.zeus.common.model;

import com.middleware.zeus.common.model.middleware.MiddlewareAlertsDTO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/5/8 1:59 下午
 */
@Accessors(chain = true)
@Data
public class MiddlewareAlertsListDto {

    @ApiModelProperty("中间件告警规则")
    private List<MiddlewareAlertsDTO> middlewareAlertsDTOList;
}
