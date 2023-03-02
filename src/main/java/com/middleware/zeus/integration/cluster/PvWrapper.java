package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.PersistentVolumeList;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/1/10 11:16 上午
 */
@Component
public class PvWrapper {

    public PersistentVolume get(String clusterId, String name){
        return K8sClient.getClient(clusterId).persistentVolumes().withName(name).get();
    }

    public List<PersistentVolume> list(String clusterId){
        PersistentVolumeList list = K8sClient.getClient(clusterId).persistentVolumes().list();
        if (list == null || CollectionUtils.isEmpty(list.getItems())) {
            return new ArrayList<>(0);
        }
        return list.getItems();
    }

}
