package com.middleware.zeus.service.k8s.impl;

import static com.middleware.caas.common.constants.NameConstant.STORAGE;
import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.STORAGE_PROVISIONER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.middleware.caas.common.model.StorageClassDTO;
import com.middleware.zeus.integration.cluster.PvcWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareInfo;
import com.middleware.zeus.service.k8s.ResourceQuotaService;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.k8s.StorageClassService;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.middleware.caas.common.model.middleware.StorageClass;
import com.middleware.zeus.integration.cluster.StorageClassWrapper;

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

    @Value("${system.backup.storageTypeCheck:true}")
    private boolean storageTypeCheck;

    @Value("#{'${system.backup.storages:localplugin.csi.alibabacloud.com}'.trim().split(',')}")
    private List<String> storageTypes;

    @Override
    public List<StorageClass> list(String clusterId, String namespace, boolean onlyMiddleware) {
        List<io.fabric8.kubernetes.api.model.storage.StorageClass> scList = scWrapper.list(clusterId);
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
        if (!storageTypeCheck) {
            return true;
        }
        List<StorageClass> list = list(clusterId, namespace, true);
        boolean isLvm = false;
        for (StorageClass sc : list) {
            if (!storageClassName.equals(sc.getName())) {
                continue;
            }
            if (StringUtils.isNotEmpty(sc.getProvisioner())
                && storageTypes.contains(sc.getProvisioner())) {
                isLvm = true;
            }
        }
        return isLvm;
    }

    @Override
    public Map<String, StorageClassDTO> convertStorageClass(List<MiddlewareInfo> pvcInfos, String clusterId,
        String namespace) {
        Map<String, StorageClassDTO> scMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(pvcInfos)) {
            pvcInfos.forEach(pvcInfo -> {
                PersistentVolumeClaim pvc = pvcWrapper.get(clusterId, namespace, pvcInfo.getName());
                if (pvc != null) {
                    StorageClassDTO sc = new StorageClassDTO();
                    String storage = pvc.getSpec().getResources().getRequests().get(STORAGE).toString();
                    String storageName = pvc.getSpec().getStorageClassName();
                    if (!CollectionUtils.isEmpty(pvc.getMetadata().getAnnotations()) && pvc.getMetadata()
                        .getAnnotations().containsKey(STORAGE_PROVISIONER)) {
                        sc.setProvisioner(
                            pvc.getMetadata().getAnnotations().get(STORAGE_PROVISIONER));
                    }
                    boolean isLvmStorage = checkLVMStorage(clusterId, namespace, storageName);
                    sc.setIsLvmStorage(isLvmStorage).setStorage(storage).setStorageClassName(storageName);
                    scMap.put(pvcInfo.getName(), sc);
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
