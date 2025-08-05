package com.middleware.zeus.service.k8s.impl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.service.k8s.SecretService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.middleware.zeus.common.model.Secret;
import com.middleware.zeus.integration.cluster.SecretWrapper;

import lombok.extern.slf4j.Slf4j;

import static com.middleware.zeus.common.constants.NameConstant.*;

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
    public List<Secret> list(String clusterId, String namespace, Map<String, String> labels) {
        List<io.fabric8.kubernetes.api.model.Secret> secretList = secretWrapper.list(clusterId, namespace, labels);
        return secretList.stream()
            .map(secret -> new Secret().setClusterId(clusterId).setNamespace(secret.getMetadata().getNamespace())
                .setName(secret.getMetadata().getName()).setData(secret.getData()).setLabels(secret.getMetadata().getLabels()))
            .collect(Collectors.toList());
    }

    @Override
    public Secret get(String clusterId, String namespace, String secretName) {
        io.fabric8.kubernetes.api.model.Secret secret = secretWrapper.get(clusterId, namespace, secretName);
        if (secret == null){
            return null;
        }
        return new Secret().setClusterId(clusterId).setNamespace(namespace).setName(secretName)
                .setData(secret.getData()).setLabels(secret.getMetadata().getLabels());
    }

    @Override
    public void create(String clusterId, String namespace, @NotNull Secret secret) {
        io.fabric8.kubernetes.api.model.Secret sc = new io.fabric8.kubernetes.api.model.Secret();
        sc.setData(secret.getData());
        ObjectMeta meta = new ObjectMeta();
        meta.setName(secret.getName());
        meta.setNamespace(secret.getNamespace());
        meta.setLabels(secret.getLabels());

        sc.setMetadata(meta);
        sc.setData(secret.getData());

        secretWrapper.create(clusterId, namespace, sc);
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

    @Override
    public String getUserConf(String clusterId, String namespace, String secretName) {
        Secret secret = this.get(clusterId, namespace, secretName);
        if (secret != null){
            Map<String, String> data = secret.getData();
            if (data.containsKey(USER_CONF)){
                return new String(Base64.getDecoder().decode(data.get(USER_CONF)));
            }
        }
        return null;
    }

    @Override
    public void genericSecretWithConf(String clusterId, String namespace, String name, String contentName, String conf) {
        Secret secret = new Secret();
        secret.setName(name);
        secret.setNamespace(namespace);
        Map<String, String> data = new HashMap<>();
        data.put(contentName, Base64.getEncoder().encodeToString(conf.getBytes(StandardCharsets.UTF_8)));

        secret.setData(data);

        this.create(clusterId, namespace, secret);
    }

    @Override
    public void genericSecretWithUsername(String clusterId, String namespace, String name, String username, String password) {
        Secret secret = new Secret();
        secret.setName(name);
        secret.setNamespace(namespace);
        Map<String, String> data = new HashMap<>();
        data.put(ACCESS_KEY, Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)));
        data.put(SECRET_KEY, Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)));

        secret.setData(data);

        this.create(clusterId, namespace, secret);
    }
}
