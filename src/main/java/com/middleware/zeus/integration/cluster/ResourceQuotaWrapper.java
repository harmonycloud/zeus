package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.api.model.ResourceQuotaList;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dengyulong
 * @date 2021/04/01
 */
@Component
public class ResourceQuotaWrapper {

    public List<ResourceQuota> list(String clusterId) {
        ResourceQuotaList list = K8sClient.getClient(clusterId).resourceQuotas().list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

    public List<ResourceQuota> list(String clusterId, String namespace) {
        ResourceQuotaList list = K8sClient.getClient(clusterId).resourceQuotas().inNamespace(namespace).list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

    public ResourceQuota get(String clusterId, String namespace, String name) {
        return K8sClient.getClient(clusterId).resourceQuotas().inNamespace(namespace).withName(name).get();
    }
    
}
