package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareList;
import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author dengyulong
 * @date 2021/03/30
 */
@Component
public class SecretWrapper {

    public void create(String clusterId, String namespace, Secret secret) {
        K8sClient.getClient(clusterId).secrets().resource(secret).create();
    }

    public void createOrReplace(String clusterId, String namespace, Secret secret) {
        K8sClient.getClient(clusterId).secrets().inNamespace(namespace).createOrReplace(secret);
    }

    public List<Secret> list(String clusterId, String namespace, Map<String, String> labels) {
        if (CollectionUtils.isEmpty(labels)){
            labels = new HashMap<>();
        }
        NonNamespaceOperation<Secret, SecretList, Resource<Secret>> secretClient = K8sClient.getClient(clusterId).secrets();
        // 条件判断
        if (StringUtils.isNotEmpty(namespace)) {
            secretClient =
                    ((MixedOperation<Secret, SecretList, Resource<Secret>>)secretClient)
                            .inNamespace(namespace);
        }
        SecretList list = secretClient.withLabels(labels).list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

    public Secret get(String clusterId, String namespace, String name) {
        return K8sClient.getClient(clusterId).secrets().inNamespace(namespace).withName(name).get();
    }

    public Secret get(String clusterId, String namespace, String labelKey, String labelValue) {
        SecretList list =
            K8sClient.getClient(clusterId).secrets().inNamespace(namespace).withLabel(labelKey, labelValue).list();
        if (!CollectionUtils.isEmpty(list.getItems())) {
            return list.getItems().get(0);
        }
        return null;
    }

    public List<Secret> list(String clusterId, String namespace, String labelKey) {
        SecretList secretList = null;
        NonNamespaceOperation<Secret, SecretList, Resource<Secret>> secretClient = K8sClient.getClient(clusterId).secrets();
        if (StringUtils.isNotEmpty(namespace)) {
            secretClient = K8sClient.getClient(clusterId).secrets().inNamespace(namespace);
        }
        if (StringUtils.isNotEmpty(labelKey)) {
            secretList = secretClient.withLabel(labelKey).list();
        } else {
            secretList = secretClient.list();
        }
        if (secretList != null) {
            return secretList.getItems();
        }
        return Collections.emptyList();
    }

}
