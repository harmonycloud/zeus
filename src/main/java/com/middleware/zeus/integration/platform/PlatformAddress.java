package com.middleware.zeus.integration.platform;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.callback.AddressSource;
import com.dtflys.forest.http.ForestAddress;
import com.dtflys.forest.http.ForestRequest;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.DisasterRecoveryInfo;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.system.PlatformService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @auther wangpenglei
 * @date 2023/3/22 15:54
 */
@Component
@Slf4j
public class PlatformAddress implements AddressSource {
    @Autowired
    private PlatformService platformService;

    @Override
    public ForestAddress getAddress(ForestRequest forestRequest) {

        DisasterRecoveryInfo disasterRecoveryInfo = platformService.getRelationPlatformAddress();
        if (disasterRecoveryInfo == null || StringUtils.isEmpty(disasterRecoveryInfo.getProtocol())
            || StringUtils.isEmpty(disasterRecoveryInfo.getHost())) {
            throw new BusinessException(ErrorMessage.NOT_EXIST);
        }

        String protocol = disasterRecoveryInfo.getProtocol();
        String host = disasterRecoveryInfo.getHost();
        Integer port = disasterRecoveryInfo.getPort();

        return new ForestAddress(protocol, host, port);
    }
}
