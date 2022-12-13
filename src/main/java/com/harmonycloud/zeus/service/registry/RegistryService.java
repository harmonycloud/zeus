package com.harmonycloud.zeus.service.registry;

import com.middleware.caas.common.model.middleware.Registry;

/**
 * @author dengyulong
 * @date 2021/05/10
 */
public interface RegistryService {

    /**
     * 校验当前制品服务
     *
     * @param registry 制品服务信息
     * @return
     */
    void validate(Registry registry);

}
