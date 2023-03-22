package com.middleware.zeus.integration.platform;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.callback.AddressSource;
import com.dtflys.forest.http.ForestAddress;
import com.dtflys.forest.http.ForestRequest;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.system.PlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @auther wangpenglei
 * @date 2023/3/22 15:54
 */
@Component
public class PlatformAddress implements AddressSource {
    @Autowired
    private HelmChartService helmChartService;
    @Override
    public ForestAddress getAddress(ForestRequest forestRequest) {
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        JSONObject relation = values.getJSONObject("args").getJSONObject("relation");
        if (relation == null) {
            throw new BusinessException(ErrorMessage.RELATION_ADDR_NOT_EXIST);
        }
        String protocol = relation.getString("protocol");
        String host = relation.getString("host");
        Integer port = relation.getInteger("port");
        return new ForestAddress(protocol,host,port);
    }
}
