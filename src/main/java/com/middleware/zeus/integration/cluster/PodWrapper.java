package com.middleware.zeus.integration.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.util.K8sClient;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;

/**
 * @author dengyulong
 * @date 2021/03/23 封装pod处理
 */
@Component
public class PodWrapper {

    public List<Pod> list(String clusterId, String namespace) {
        // init client
        NonNamespaceOperation<Pod, PodList, PodResource> podClient = K8sClient.getClient(clusterId).pods();
        if (StringUtils.isNotEmpty(namespace)) {
            podClient = ((MixedOperation<Pod, PodList, PodResource>)podClient).inNamespace(namespace);
        }
        PodList list = podClient.list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

    public List<Pod> list(String clusterId, String namespace, Map<String, String> labels) {
        if (CollectionUtils.isEmpty(labels)){
            labels = new HashMap<>();
        }
        // init client
        NonNamespaceOperation<Pod, PodList, PodResource> podClient = K8sClient.getClient(clusterId).pods();
        if (StringUtils.isNotEmpty(namespace)) {
            podClient = ((MixedOperation<Pod, PodList, PodResource>)podClient).inNamespace(namespace);
        }

        PodList list = podClient.withLabels(labels).list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

    public List<Pod> listByFields(String clusterId, String namespace, Map<String, String> fields) {
        if (CollectionUtils.isEmpty(fields)){
            fields = new HashMap<>();
        }
        // init client
        NonNamespaceOperation<Pod, PodList, PodResource> podClient = K8sClient.getClient(clusterId).pods();
        if (StringUtils.isNotEmpty(namespace)) {
            podClient = ((MixedOperation<Pod, PodList, PodResource>)podClient).inNamespace(namespace);
        }
        PodList list = podClient.withFields(fields).list();
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
