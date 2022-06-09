package com.harmonycloud.zeus.service.k8s.impl;

import com.harmonycloud.caas.common.model.PersistentVolumeClaim;
import com.harmonycloud.zeus.integration.cluster.PvcWrapper;
import com.harmonycloud.zeus.service.k8s.PvcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author dengyulong
 * @date 2021/04/01
 */
@Service
public class PvcServiceImpl implements PvcService {

    @Autowired
    private PvcWrapper pvcWrapper;

    @Override
    public List<PersistentVolumeClaim> list(String clusterId, String namespace) {
        List<io.fabric8.kubernetes.api.model.PersistentVolumeClaim> pvcList = pvcWrapper.list(clusterId, namespace);
        return pvcList.stream().map(pvc -> convert(pvc).setClusterId(clusterId).setNamespace(namespace))
            .collect(Collectors.toList());
    }

    @Override
    public List<PersistentVolumeClaim> listWithFields(String clusterId, String namespace, Map<String, String> fields) {
        List<io.fabric8.kubernetes.api.model.PersistentVolumeClaim> pvcList =
            pvcWrapper.listWithFields(clusterId, namespace, fields);
        return pvcList.stream().map(pvc -> convert(pvc).setClusterId(clusterId).setNamespace(namespace))
            .collect(Collectors.toList());
    }

    /**
     * 封装pvc
     */
    public PersistentVolumeClaim convert(io.fabric8.kubernetes.api.model.PersistentVolumeClaim pvc) {
        PersistentVolumeClaim persistentVolumeClaim =
            new PersistentVolumeClaim().setName(pvc.getMetadata().getName()).setLabels(pvc.getMetadata().getLabels())
                .setAccessModes(pvc.getSpec().getAccessModes()).setStorageClassName(pvc.getSpec().getStorageClassName())
                .setVolumeMode(pvc.getSpec().getVolumeMode()).setPhase(pvc.getStatus().getPhase());
        if (pvc.getSpec().getResources().getRequests() != null
            && pvc.getSpec().getResources().getRequests().containsKey("storage")) {
            persistentVolumeClaim.setRequest(pvc.getSpec().getResources().getRequests().get("storage").toString());
        }
        return persistentVolumeClaim;
    }

}
