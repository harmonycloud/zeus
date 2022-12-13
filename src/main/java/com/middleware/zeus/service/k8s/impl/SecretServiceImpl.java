package com.middleware.zeus.service.k8s.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.zeus.service.k8s.SecretService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.middleware.caas.common.model.Secret;
import com.middleware.zeus.integration.cluster.SecretWrapper;

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

    @Override
    public Secret get(String clusterId, String namespace, String secretName) {
        io.fabric8.kubernetes.api.model.Secret secret = secretWrapper.get(clusterId, namespace, secretName);
        if (secret == null){
            return null;
        }
        return new Secret().setClusterId(clusterId).setNamespace(namespace).setName(secretName)
            .setData(secret.getData());
    }

    @Override
    public void create(String clusterId, String namespace, Secret secret) {
        try {
            io.fabric8.kubernetes.api.model.Secret sc = new io.fabric8.kubernetes.api.model.Secret();
            sc.setData(secret.getData());
            ObjectMeta meta = new ObjectMeta();
            meta.setName(secret.getName());
            meta.setNamespace(secret.getNamespace());
            meta.setLabels(secret.getLabels());

            sc.setMetadata(meta);
            sc.setData(secret.getData());

            secretWrapper.create(clusterId, namespace, sc);
        }catch (Exception e){
            throw new BusinessException(ErrorMessage.NOT_EXIST);
        }
    }

    @Override
    public void createOrReplace(String clusterId, String namespace, Secret secret){
        io.fabric8.kubernetes.api.model.Secret sc = secretWrapper.get(clusterId, namespace, secret.getName());
        if (sc != null){
            sc.setData(secret.getData());
        }
        createOrReplace(clusterId, namespace, sc);
    }

    @Override
    public void createOrReplace(String clusterId, String namespace, io.fabric8.kubernetes.api.model.Secret secret){
        secretWrapper.createOrReplace(clusterId, namespace, secret);
    }
}