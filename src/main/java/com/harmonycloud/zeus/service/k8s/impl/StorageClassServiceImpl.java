package com.harmonycloud.zeus.service.k8s.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.DISK;
import static com.harmonycloud.caas.common.constants.NameConstant.STORAGE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.harmonycloud.caas.common.model.StorageClassDTO;
import com.harmonycloud.zeus.integration.cluster.PvcWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.service.k8s.ResourceQuotaService;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.harmonycloud.caas.common.enums.middleware.ResourceUnitEnum;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.StorageClass;
import com.harmonycloud.zeus.integration.cluster.StorageClassWrapper;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.StorageClassService;
import com.harmonycloud.tool.numeric.ResourceCalculationUtil;

import org.springframework.util.CollectionUtils;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
@Service
public class StorageClassServiceImpl implements StorageClassService {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private StorageClassWrapper scWrapper;
    @Autowired
    private ResourceQuotaService resourceQuotaService;
    @Autowired
    private PvcWrapper pvcWrapper;

    @Override
    public List<StorageClass> list(String clusterId, String namespace, boolean onlyMiddleware) {
        List<io.fabric8.kubernetes.api.model.storage.StorageClass> scList = scWrapper.list(clusterId);
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        if (CollectionUtils.isEmpty(cluster.getStorage())) {
            return new ArrayList<>(0);
        }
        Map<String, Object> support = (Map<String, Object>)cluster.getStorage().get("support");
        List<StorageClass> list = new ArrayList<>();

        // 取出存储配额
        Map<String, List<String>> rqMap;
        if (StringUtils.isNotBlank(namespace)) {
            rqMap = resourceQuotaService.get(clusterId, namespace, namespace + "quota");
        } else {
            rqMap = resourceQuotaService.statistics(clusterId);
        }

        for (io.fabric8.kubernetes.api.model.storage.StorageClass sc : scList) {
            StorageClass s = new StorageClass().setName(sc.getMetadata().getName())
                .setLabels(sc.getMetadata().getLabels()).setParameters(sc.getParameters())
                .setProvisioner(sc.getProvisioner()).setReclaimPolicy(sc.getReclaimPolicy())
                .setVolumeBindingMode(sc.getVolumeBindingMode());
            // 设置配额
            s.setStorageLimit(support.get(s.getType()) == null ? null : String.valueOf(ResourceCalculationUtil
                .getResourceValue(support.get(s.getType()).toString(), DISK, ResourceUnitEnum.GI.getUnit())));

            List<String> quotas = rqMap.get(s.getName());
            if (!CollectionUtils.isEmpty(quotas)) {
                s.setStorageQuota(quotas.get(1));
                s.setStorageUsed(quotas.get(2));
            }
            list.add(s);
        }
        return list;
    }

    @Override
    public boolean checkLVMStorage(String clusterId, String namespace, String storageClassName) {
        if (StringUtils.isBlank(clusterId) || StringUtils.isBlank(namespace) || StringUtils.isBlank(storageClassName)) {
            return false;
        }
        List<StorageClass> list = list(clusterId, namespace, true);
        boolean isLvm = false;
        for (StorageClass sc : list) {
            if (!storageClassName.equals(sc.getName())) {
                continue;
            }
            Map<String, String> parameters = sc.getParameters();
            if (parameters != null && parameters.get("volumeType") != null && "LVM".equalsIgnoreCase(parameters.get("volumeType"))) {
                isLvm = true;
            }
        }
        return isLvm;
    }

    @Override
    public Map<String, StorageClassDTO> convertStorageClass(List<MiddlewareInfo> pvcInfos, String clusterId, String namespace) {
        Map<String, StorageClassDTO> scMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(pvcInfos)) {
            pvcInfos.forEach(pvcInfo -> {
                PersistentVolumeClaim pvc = pvcWrapper.get(clusterId, namespace, pvcInfo.getName());
                if (pvc != null) {
                    String storage = pvc.getSpec().getResources().getRequests().get(STORAGE).toString();
                    String storageName = pvc.getSpec().getStorageClassName();
                    boolean isLvmStorage = checkLVMStorage(clusterId, namespace, storageName);
                    scMap.put(pvcInfo.getName(), new StorageClassDTO(storage, storageName, isLvmStorage));
                }
            });
        }
        return scMap;
    }

    @Override
    public StorageClassDTO fuzzySearchStorageClass(Map<String, StorageClassDTO> scMap, String keyword) {
        AtomicReference<StorageClassDTO> sc = new AtomicReference<>();
        scMap.forEach((k, v) -> {
            if (k.contains(keyword)) {
                sc.set(v);
            }
        });
        return sc.get();
    }
}
