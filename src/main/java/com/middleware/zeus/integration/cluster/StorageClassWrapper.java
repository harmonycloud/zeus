package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.api.model.storage.StorageClassList;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class StorageClassWrapper {

    public List<StorageClass> list(String clusterId) {
        StorageClassList list = K8sClient.getClient(clusterId).storage().storageClasses().list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

    public List<StorageClass> list(String clusterId, Map<String, String> labels) {
        StorageClassList list = K8sClient.getClient(clusterId).storage().storageClasses().withLabels(labels).list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

    public StorageClass get(String clusterId, String storageClassName) {
        return K8sClient.getClient(clusterId).storage().storageClasses().withName(storageClassName).get();
    }

    public void update(String clusterId, StorageClass storageClass) {
        try {
            K8sClient.getClient(clusterId).storage().storageClasses().createOrReplace(storageClass);
        } catch (Exception e){
            log.info("集群{} storageClass更新失败", clusterId);
            throw e;
        }
    }

}
