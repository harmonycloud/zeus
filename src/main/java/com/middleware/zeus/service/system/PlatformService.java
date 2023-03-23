package com.middleware.zeus.service.system;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.model.DisasterRecoveryDto;
import com.middleware.caas.common.model.DisasterRecoveryInfo;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @auther wangpenglei
 * @date 2023/3/22 10:33
 */
public interface PlatformService {

    DisasterRecoveryDto queryAccessInfo();

    void switchPlatform(HttpServletRequest request) throws IOException;

    void saveAddr(DisasterRecoveryInfo info, String name);
}
