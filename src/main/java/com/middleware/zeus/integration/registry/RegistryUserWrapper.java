package com.middleware.zeus.integration.registry;

import com.middleware.zeus.integration.registry.api.harbor.V1UserApi;
import com.middleware.zeus.integration.registry.bean.harbor.V1CurrentUser;
import com.middleware.zeus.util.ExceptionUtils;
import org.springframework.stereotype.Component;

import com.middleware.caas.common.model.middleware.Registry;
import com.middleware.zeus.integration.registry.client.RegistryClientFactory;
import com.middleware.tool.api.common.ApiException;

/**
 * @author dengyulong
 * @date 2021/05/16
 */
@Component
public class RegistryUserWrapper {
    
    public V1CurrentUser getCurrentUser(Registry registry) {
        V1UserApi userApi = new V1UserApi(RegistryClientFactory.getClient(registry));
        try {
            return userApi.getCurrentUser(null);
        } catch (ApiException e) {
            throw ExceptionUtils.convertRegistryApiExp2BuzExp(e);
        }
    }
    
}
