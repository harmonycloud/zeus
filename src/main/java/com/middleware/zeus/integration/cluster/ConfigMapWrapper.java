package com.middleware.zeus.integration.cluster;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;

/**
 * @author tangtx
 * @date 2021/03/26 11:00 AM
 */
@Slf4j
@Component
public class ConfigMapWrapper {

    public List<ConfigMap> list(String clusterId, String namespace) {
        ConfigMapList configMapList = K8sClient.getClient(clusterId).configMaps().inNamespace(namespace).list();
        if (CollectionUtils.isEmpty(configMapList.getItems())) {
            return null;
        }
        return configMapList.getItems();
    }

    public ConfigMap create(String clusterId, String namespace, ConfigMap configMap) {
        ConfigMap res = K8sClient.getClient(clusterId).configMaps().inNamespace(namespace).create(configMap);
        return res;
    }

    public ConfigMap create(String clusterId, String namespace, String name, Map<String, String> data) {
        ConfigMap cm = new ConfigMap();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(name);
        metadata.setNamespace(namespace);
        cm.setMetadata(metadata);
        cm.setData(data);
        try {
            return K8sClient.getClient(clusterId).configMaps().inNamespace(namespace).create(cm);
        } catch (KubernetesClientException e) {
            if (e.getCode() == 409) {
                log.debug("集群{}创建配置文件{}/{}，已存在", clusterId, namespace, name);
                return cm;
            }
            throw e;
        }
    }

    public ConfigMap update(String clusterId, String namespace, ConfigMap configMap) {
        ConfigMap res = K8sClient.getClient(clusterId).configMaps().inNamespace(namespace).createOrReplace(configMap);
        return res;
    }

    public boolean delete(String clusterId, String namespace, String name) {
        boolean res = K8sClient.getClient(clusterId).configMaps().inNamespace(namespace).withName(name).delete();
        return res;
    }

    public ConfigMap get(String clusterId, String namespace, String name) {
        ConfigMap res = K8sClient.getClient(clusterId).configMaps().inNamespace(namespace).withName(name).get();
        return res;
    }

}
