package com.harmonycloud.zeus.integration.cluster;

import com.harmonycloud.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.DoneableNamespace;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Component
public class NamespaceWrapper {

    public List<Namespace> list(String clusterId) {
        return list(clusterId, null, null);
    }

    public List<Namespace> list(String clusterId, String labelKey) {
        return list(clusterId, labelKey, null);
    }

    public List<Namespace> list(String clusterId, String labelKey, String labelValue) {
        NamespaceList list;
        if (StringUtils.isEmpty(labelKey)) {
            list = K8sClient.getClient(clusterId).namespaces().list();
        } else if (StringUtils.isEmpty(labelValue)) {
            list = K8sClient.getClient(clusterId).namespaces().withLabel(labelKey).list();
        } else {
            list = K8sClient.getClient(clusterId).namespaces().withLabel(labelKey, labelValue).list();
        }
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

    public Namespace get(String clusterId, String namespace){
        return K8sClient.getClient(clusterId).namespaces().withName(namespace).get();
    }

    public void save(String clusterId, Namespace ns) {
        K8sClient.getClient(clusterId).namespaces().createOrReplace(ns);
    }

    public void delete(String clusterId, Namespace ns) {
        K8sClient.getClient(clusterId).namespaces().delete(ns);
    }

    public Namespace get(String clusterId,String name){
        return K8sClient.getClient(clusterId).namespaces().withName(name).get();
    }

}
