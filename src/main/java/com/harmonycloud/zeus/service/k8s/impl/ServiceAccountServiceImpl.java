package com.harmonycloud.zeus.service.k8s.impl;

import com.harmonycloud.zeus.integration.cluster.ServiceAccountWrapper;
import com.harmonycloud.zeus.service.k8s.ServiceAccountService;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2022/12/1 8:32 下午
 */
@Service
public class ServiceAccountServiceImpl implements ServiceAccountService {

    @Autowired
    private ServiceAccountWrapper serviceAccountWrapper;

    @Override
    public ServiceAccount get(String clusterId, String namespace, String name) {
        return serviceAccountWrapper.get(clusterId, namespace, name);
    }

    @Override
    public void bindImagePullSecret(String clusterId, String namespace, ServiceAccount sa, List<Secret> secrets) {
        List<LocalObjectReference> imagePullSecrets = sa.getImagePullSecrets();
        secrets = secrets.stream().filter(secret -> imagePullSecrets.stream().
                noneMatch(imagePullSecret -> secret.getMetadata().getName().equals(imagePullSecret.getName()))).
                collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(secrets)) {
            secrets.forEach(secret -> {
                imagePullSecrets.add(new LocalObjectReference(secret.getMetadata().getName()));
            });
            serviceAccountWrapper.createOrReplace(clusterId, sa);
        }
    }

}
