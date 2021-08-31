package com.harmonycloud.zeus.integration.cluster;

import com.harmonycloud.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
@Component
public class PvcWrapper {

    public void deleteByLabels(String clusterId, String namespace, String labels) {
        K8sClient.getClient(clusterId).persistentVolumeClaims().inNamespace(namespace).withLabel(labels).delete();
    }
    
    public List<PersistentVolumeClaim> list(String clusterId, String namespace) {
        PersistentVolumeClaimList list =
            K8sClient.getClient(clusterId).persistentVolumeClaims().inNamespace(namespace).list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

    public PersistentVolumeClaim get(String clusterId, String namespace, String name) {
        return K8sClient.getClient(clusterId).persistentVolumeClaims().inNamespace(namespace).withName(name).get();
    }

    public void delete(String clusterId, String namespace, String name) {
        K8sClient.getClient(clusterId).persistentVolumeClaims().inNamespace(namespace).withName(name).delete();
    }

}
