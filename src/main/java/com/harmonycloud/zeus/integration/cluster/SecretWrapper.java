package com.harmonycloud.zeus.integration.cluster;

import com.harmonycloud.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/30
 */
@Component
public class SecretWrapper {

    public void create(String clusterId, String namespace, Secret secret) {
        K8sClient.getClient(clusterId).secrets().inNamespace(namespace).create(secret);
    }

    public void createOrReplace(String clusterId, String namespace, Secret secret) {
        K8sClient.getClient(clusterId).secrets().inNamespace(namespace).createOrReplace(secret);
    }

    public List<Secret> list(String clusterId, String namespace) {
        SecretList list = K8sClient.getClient(clusterId).secrets().inNamespace(namespace).list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }
    
    public Secret get(String clusterId, String namespace, String name) {
        return K8sClient.getClient(clusterId).secrets().inNamespace(namespace).withName(name).get();
    }
    
}
