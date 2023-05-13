package com.middleware.zeus.common.model;

import com.middleware.zeus.common.model.middleware.MiddlewareAlertsDTO;
import com.middleware.zeus.common.model.user.UserDto;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author yushuaikang
 * @date 2021/12/24 下午3:35
 */
@Accessors(chain = true)
@Data
public class AlertsUserDTO {

    private List<MiddlewareAlertsDTO> middlewareAlertsDTOList;

    private List<UserDto> users;
}
