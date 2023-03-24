package com.middleware.zeus.service.system;

import com.middleware.caas.common.model.DisasterRecoveryDto;
import com.middleware.caas.common.model.DisasterRecoveryInfo;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @auther wangpenglei
 * @date 2023/3/22 10:33
 */
public interface PlatformService {

    DisasterRecoveryDto queryAccessInfo(HttpServletRequest request);

    void switchPlatform(HttpServletRequest request) throws IOException;

    void saveAddr(DisasterRecoveryInfo info, String name, HttpServletRequest request);

    DisasterRecoveryDto getMysqlReplicateStatus();

    String getMiddlewareUid();
}
