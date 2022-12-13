package com.middleware.zeus.service.k8s.impl;

import com.middleware.zeus.integration.cluster.ConfigMapWrapper;
import com.middleware.zeus.service.k8s.ConfigMapService;
import io.fabric8.kubernetes.api.model.ConfigMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author tangtx
 * @date 3/26/21 9:56 AM
 */
@Service
public class ConfigMapServiceImpl implements ConfigMapService {

    @Autowired
    private ConfigMapWrapper configMapWrapper;

    @Override
    public ConfigMap get(String clusterId, String namespace, String name) {
        return configMapWrapper.get(clusterId, namespace, name);
    }

    @Override
    public void create(String clusterId, String namespace, ConfigMap configMap) {
        configMapWrapper.create(clusterId, namespace, configMap);
    }

    @Override
    public void create(String clusterId, String namespace, String name, Map<String, String> data) {
        configMapWrapper.create(clusterId, namespace, name, data);
    }

    @Override
    public void delete(String clusterId, String namespace, String name) {
        configMapWrapper.delete(clusterId, namespace, name);
    }

    @Override
    public void update(String clusterId, String namespace, ConfigMap configMap) {
        configMapWrapper.update(clusterId, namespace, configMap);
    }
}
