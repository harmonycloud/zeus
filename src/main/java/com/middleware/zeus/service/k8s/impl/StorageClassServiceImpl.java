package com.middleware.zeus.service.k8s.impl;

import static com.middleware.zeus.common.constants.NameConstant.STORAGE;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.STORAGE_PROVISIONER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.middleware.zeus.common.model.StorageDto;
import com.middleware.zeus.integration.cluster.StorageClassWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareInfo;
import com.middleware.zeus.service.k8s.PvcService;
import com.middleware.zeus.service.k8s.ResourceQuotaService;
import com.middleware.zeus.service.k8s.StorageClassService;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.common.model.ResourceQuotaDo;
import com.middleware.zeus.common.model.StorageClassDTO;
import com.middleware.zeus.common.model.StorageQuota;
import com.middleware.zeus.common.model.middleware.StorageClassInfo;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;

/**
 * @author dengyulong
 * @date 2021/03/31
 */
@Service
public class StorageClassServiceImpl implements StorageClassService {

    @Autowired
    private StorageClassWrapper scWrapper;
    @Autowired
    private ResourceQuotaService resourceQuotaService;
    @Autowired
    private PvcService pvcService;

    @Value("${system.backup.storageTypeCheck:true}")
    private boolean storageTypeCheck;

    @Value("#{'${system.backup.storages:localplugin.csi.alibabacloud.com}'.trim().split(',')}")
    private List<String> storageTypes;

    @Override
    public List<StorageClassInfo> list(String clusterId, String namespace, boolean onlyMiddleware) {
        List<StorageClass> scList = scWrapper.list(clusterId);
        List<StorageClassInfo> list = new ArrayList<>();

        // 取出存储配额
        ResourceQuotaDo resourceQuotaDo;
        if (StringUtils.isNotBlank(namespace)) {
            resourceQuotaDo = resourceQuotaService.get(clusterId, namespace, namespace + "quota");
        } else {
            resourceQuotaDo = resourceQuotaService.statistics(clusterId);
        }

        for (StorageClass sc : scList) {
            StorageClassInfo s = new StorageClassInfo().setName(sc.getMetadata().getName())
                .setLabels(sc.getMetadata().getLabels()).setParameters(sc.getParameters())
                .setProvisioner(sc.getProvisioner()).setReclaimPolicy(sc.getReclaimPolicy())
                .setVolumeBindingMode(sc.getVolumeBindingMode());

            if (!CollectionUtils.isEmpty(resourceQuotaDo.getStorageList())){
                List<StorageQuota> storageQuotaList = resourceQuotaDo.getStorageList().stream().filter(storageQuota -> storageQuota.getName().equals(s.getName())).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(storageQuotaList)){
                    s.setStorageQuota(String.valueOf(storageQuotaList.get(0).getStorage().getRequest()));
                    s.setStorageUsed(String.valueOf(storageQuotaList.get(0).getStorage().getUsed()));
                }
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
        List<StorageClassInfo> list = list(clusterId, namespace, true);
        boolean isLvm = false;
        for (StorageClassInfo sc : list) {
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
    public boolean checkLVMStorage(StorageDto storageDto) {
        if (!storageTypeCheck) {
            return true;
        }
        if (!CollectionUtils.isEmpty(storageDto.getStorageClassList())){
            StorageClassInfo sc = storageDto.getStorageClassList().get(0);
            if (StringUtils.isNotEmpty(sc.getProvisioner())
                    && storageTypes.contains(sc.getProvisioner())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Map<String, StorageClassDTO> convertStorageClass(List<MiddlewareInfo> pvcInfos, String clusterId,
                                                            String namespace) {
        Map<String, StorageClassDTO> scMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(pvcInfos)) {
            pvcInfos.forEach(pvcInfo -> {
                PersistentVolumeClaim pvc = pvcService.get(clusterId, namespace, pvcInfo.getName());
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
    public List<StorageClassDTO> fuzzySearchStorageClass(Map<String, StorageClassDTO> scMap, String keyword) {
        List<StorageClassDTO> list = new ArrayList<>();
        scMap.forEach((k, v) -> {
            if (k.contains(keyword)) {
                list.add(v);
            }
        });
        return list;
    }

}
