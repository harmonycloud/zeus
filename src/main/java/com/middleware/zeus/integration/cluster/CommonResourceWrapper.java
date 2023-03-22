package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CommonResourceWrapper {

    public  String getYaml(String clusterId, String namespace, String plural, String name) {
        Object res = null;
        switch (plural){
            case "deployments":
                res = K8sClient.getClient(clusterId).apps().deployments().inNamespace(namespace).withName(name).get();
                break;
            case "statefulsets":
                res = K8sClient.getClient(clusterId).apps().statefulSets().inNamespace(namespace).withName(name).get();
                break;
            case "pods":
                res = K8sClient.getClient(clusterId).pods().inNamespace(namespace).withName(name).get();
                break;
            case "persistentvolumeclaims":
                res = K8sClient.getClient(clusterId).persistentVolumeClaims().inNamespace(namespace).withName(name).get();
                break;
            case "services":
                res = K8sClient.getClient(clusterId).services().inNamespace(namespace).withName(name).get();
                break;
        }
        Yaml yaml = new Yaml();
        return yaml.dumpAsMap(res);
    }

}
