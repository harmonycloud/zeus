package com.middleware.zeus.service.k8s.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.middleware.zeus.integration.cluster.SecretWrapper;
import com.middleware.zeus.service.k8s.SecretService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.middleware.caas.common.model.Secret;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @since 2021/6/23 10:56 上午
 */
@Service
@Slf4j
public class SecretServiceImpl implements SecretService {

    @Autowired
    private SecretWrapper secretWrapper;

    @Override
    public List<Secret> list(String clusterId, String namespace) {
        List<io.fabric8.kubernetes.api.model.Secret> secretList = secretWrapper.list(clusterId, namespace);
        return secretList.stream().map(secret -> new Secret().setClusterId(clusterId).setNamespace(namespace)
            .setName(secret.getMetadata().getName()).setData(secret.getData())).collect(Collectors.toList());
    }
}
