package com.middleware.zeus.integration.cluster;

import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public List<PersistentVolumeClaim> listWithFields(String clusterId, String namespace, Map<String, String> fields){
        PersistentVolumeClaimList list =
                K8sClient.getClient(clusterId).persistentVolumeClaims().inNamespace(namespace).withFields(fields).list();
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

    public void delete(String clusterId, String namespace, Map<String, String> map) {
        K8sClient.getClient(clusterId).persistentVolumeClaims().inNamespace(namespace).withLabels(map).delete();
    }

    public void update(String clusterId, String namespace, PersistentVolumeClaim pvc) {
        try {
            K8sClient.getClient(clusterId).persistentVolumeClaims().inNamespace(namespace).createOrReplace(pvc);
        } catch (Exception e) {
            if (StringUtils.isNotEmpty(e.getMessage())
                && e.getMessage().contains("field can not be less than previous value")) {
                throw new BusinessException(ErrorMessage.PVC_CAN_LESS_THAN_PREVIOUS);
            } else {
                throw e;
            }
        }
    }

}
