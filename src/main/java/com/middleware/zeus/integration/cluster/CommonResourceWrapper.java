package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.util.K8sClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Slf4j
@Component
public class CommonResourceWrapper {

    @Autowired
    private IngressWrapper ingressWrapper;

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
            case "ingresses":
                res = ingressWrapper.get(clusterId, namespace, name);
                break;
            default:
                res = "暂不支持查看该类型yaml";
        }
        Yaml yaml = new Yaml();
        return yaml.dumpAsMap(res);
    }

}
