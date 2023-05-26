package com.middleware.zeus.service.k8s.impl;

import com.middleware.zeus.common.enums.middleware.ResourceUnitEnum;
import com.middleware.zeus.common.model.PersistentVolumeClaim;
import com.middleware.zeus.util.date.DateUtils;
import com.middleware.zeus.util.numeric.ResourceCalculationUtil;
import com.middleware.zeus.integration.cluster.PvcWrapper;
import com.middleware.zeus.service.k8s.PvcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.middleware.zeus.common.constants.NameConstant.MEMORY;
import static com.middleware.zeus.common.constants.NameConstant.STORAGE;

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
    public io.fabric8.kubernetes.api.model.PersistentVolumeClaim get(String clusterId, String namespace, String name) {
        return pvcWrapper.get(clusterId, namespace, name);
    }

    @Override
    public void update(String clusterId, String namespace, io.fabric8.kubernetes.api.model.PersistentVolumeClaim pvc) {
        pvcWrapper.update(clusterId, namespace, pvc);
    }

    @Override
    public boolean checkPvcExist(String clusterId, String namespace, String... pvcs) {
        for (int i = 0; i < pvcs.length; ++i) {
            io.fabric8.kubernetes.api.model.PersistentVolumeClaim pvc = pvcWrapper.get(clusterId, namespace, pvcs[i]);
            if (pvc != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<PersistentVolumeClaim> listWithFields(String clusterId, String namespace, Map<String, String> fields) {
        List<io.fabric8.kubernetes.api.model.PersistentVolumeClaim> pvcList =
            pvcWrapper.listWithFields(clusterId, namespace, fields);
        return pvcList.stream().map(pvc -> convert(pvc).setClusterId(clusterId).setNamespace(namespace))
            .collect(Collectors.toList());
    }

    @Override
    public List<PersistentVolumeClaim> listWithLabels(String clusterId, String namespace, Map<String, String> labels) {
        List<io.fabric8.kubernetes.api.model.PersistentVolumeClaim> pvcList =
                pvcWrapper.listWithLabels(clusterId, namespace, labels);
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
                .setVolumeMode(pvc.getSpec().getVolumeMode()).setPhase(pvc.getStatus().getPhase())
                .setCreateTime(DateUtils.parseUTCDate(pvc.getMetadata().getCreationTimestamp())).setVolumeName(pvc.getSpec().getVolumeName());
        if (pvc.getSpec().getResources().getRequests() != null
            && pvc.getSpec().getResources().getRequests().containsKey(STORAGE)) {
            double request = ResourceCalculationUtil.getResourceValue(pvc.getSpec().getResources().getRequests().get(STORAGE).toString(), MEMORY, ResourceUnitEnum.GI.getUnit());
            persistentVolumeClaim.setRequest(request);
        }
        if (pvc.getStatus() != null && pvc.getStatus().getCapacity() != null && pvc.getStatus().getCapacity().containsKey(STORAGE)){
            double capacity = ResourceCalculationUtil.getResourceValue(pvc.getStatus().getCapacity().get(STORAGE).toString(), MEMORY, ResourceUnitEnum.GI.getUnit());
            persistentVolumeClaim.setCapacity(capacity);
        }
        return persistentVolumeClaim;
    }

}
