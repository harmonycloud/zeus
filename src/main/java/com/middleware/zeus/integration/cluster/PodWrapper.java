package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dengyulong
 * @date 2021/03/23
 * 封装pod处理
 */
@Component
public class PodWrapper {

    public List<Pod> list(String clusterId, String namespace) {
        PodList list = K8sClient.getClient(clusterId).pods().inNamespace(namespace).list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

    public List<Pod> list(String clusterId, String namespace, Map<String, String> labels) {
        if (CollectionUtils.isEmpty(labels)){
            labels = new HashMap<>(0);
        }
        PodList list = K8sClient.getClient(clusterId).pods().inNamespace(namespace).withLabels(labels).list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

    public List<Pod> listByFields(String clusterId, String namespace, Map<String, String> fields) {
        if (CollectionUtils.isEmpty(fields)){
            fields = new HashMap<>(0);
        }
        PodList list = K8sClient.getClient(clusterId).pods().inNamespace(namespace).withFields(fields).list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

    public Pod get(String clusterId, String namespace, String name) {
        return K8sClient.getClient(clusterId).pods().inNamespace(namespace).withName(name).get();
    }

    public void delete(String clusterId, String namespace, String name) {
        K8sClient.getClient(clusterId).pods().inNamespace(namespace).withName(name).delete();
    }

}
