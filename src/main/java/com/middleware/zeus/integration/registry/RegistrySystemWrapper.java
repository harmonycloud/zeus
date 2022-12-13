package com.middleware.zeus.integration.registry;

import com.middleware.zeus.integration.registry.api.harbor.V1SystemApi;
import com.middleware.zeus.util.ExceptionUtils;
import org.springframework.stereotype.Component;

import com.middleware.caas.common.model.middleware.Registry;
import com.middleware.zeus.integration.registry.bean.harbor.V1SystemInfo;
import com.middleware.zeus.integration.registry.client.RegistryClientFactory;
import com.middleware.tool.api.common.ApiException;

/**
 * @author dengyulong
 * @date 2021/05/24
 */
@Component
public class RegistrySystemWrapper {
    
    public V1SystemInfo getSystemInfo(Registry registry) {
        V1SystemApi systemApi = new V1SystemApi(RegistryClientFactory.getClient(registry));
        try {
            return systemApi.getSystemInfo();
        } catch (ApiException e) {
            throw ExceptionUtils.convertRegistryApiExp2BuzExp(e);
        }
    }

}
