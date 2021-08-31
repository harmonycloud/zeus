package com.harmonycloud.zeus.service.registry.impl;

import com.harmonycloud.zeus.integration.registry.RegistrySystemWrapper;
import com.harmonycloud.zeus.integration.registry.RegistryUserWrapper;
import com.harmonycloud.zeus.integration.registry.bean.harbor.V1CurrentUser;
import com.harmonycloud.zeus.integration.registry.bean.harbor.V1SystemInfo;
import com.harmonycloud.zeus.service.registry.AbstractRegistryService;
import com.harmonycloud.zeus.service.registry.RegistryService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.registry.RegistryType;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.Registry;

import lombok.extern.slf4j.Slf4j;

/**
 * @author dengyulong
 * @date 2021/05/10
 */
@Slf4j
@Service
public class RegistryServiceImpl extends AbstractRegistryService implements RegistryService {

    @Autowired
    private RegistryUserWrapper registryUserWrapper;
    @Autowired
    private RegistrySystemWrapper registrySystemWrapper;

    @Override
    public void validate(Registry registry) {
        // 已知制品服务版本
        if (StringUtils.isNotEmpty(registry.getVersion())) {
            if (RegistryType.HARBOR.getType().equals(registry.getType()) && validateHarbor(registry)) {
                return;
            }
            if (RegistryType.JFROG.getType().equals(registry.getType()) && !validateJfrog(registry)) {
                return;
            }
            throw new BusinessException(DictEnum.REGISTRY, registry.getRegistryUrl(), ErrorMessage.VALIDATE_FAILED);
        }
        // 未知版本，挨个尝试
        // harbor
        if (RegistryType.HARBOR.getType().equals(registry.getType())) {
            // 校验harbor v1
            registry.setVersion("v1");
            if (validateHarbor(registry)) {
                return;
            }
            // 校验harbor v2
            registry.setVersion("v2");
            if (validateHarbor(registry)) {
                return;
            }
        } else if (RegistryType.JFROG.getType().equals(registry.getType())) {
            // 校验jfrog v2
            registry.setVersion("v2");
            if (validateJfrog(registry)) {
                return;
            }
        }

        throw new BusinessException(DictEnum.REGISTRY, registry.getRegistryUrl(), ErrorMessage.VALIDATE_FAILED);
    }

    private boolean validateHarbor(Registry registry) {
        try {
            V1CurrentUser currentUser = registryUserWrapper.getCurrentUser(registry);
            if (currentUser != null) {
                V1SystemInfo systemInfo = registrySystemWrapper.getSystemInfo(registry);
                if (systemInfo != null) {
                    if (StringUtils.isEmpty(registry.getVersion()) || !registry.getVersion().contains(".")) {
                        registry.setVersion(systemInfo.getHarborVersion());
                    }
                    return true;
                }
            }
        } catch (BusinessException e) {
            // 如果是未授权，api没问题，账号密码信息错误
            if (e.isUnauthorized()) {
                throw new BusinessException(DictEnum.REGISTRY, registry.getRegistryUrl(), ErrorMessage.VALIDATE_FAILED);
            }
            log.error("校验制品服务异常 {} ({}) : {}", registry.getRegistryAddress(), registry.getVersion(), e.getDetail());
        } catch (Exception e) {
            log.error("校验制品服务异常 {}", registry.getRegistryAddress());
        }
        return false;
    }

    private boolean validateJfrog(Registry registry) {
        // todo check jfrog
        return false;
    }

}
