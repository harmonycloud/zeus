package com.harmonycloud.zeus.service.k8s.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.DISK;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.harmonycloud.zeus.service.k8s.ResourceQuotaService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.harmonycloud.caas.common.enums.middleware.ResourceUnitEnum;
import com.harmonycloud.caas.common.enums.middleware.StorageClassProvisionerEnum;
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

    @Override
    public List<StorageClass> list(String clusterId, String namespace, boolean onlyMiddleware) {
        List<io.fabric8.kubernetes.api.model.storage.StorageClass> scList = scWrapper.list(clusterId);
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        if (CollectionUtils.isEmpty(cluster.getStorage())) {
            return new ArrayList<>(0);
        }
        Map<String, Object> support = (Map<String, Object>)cluster.getStorage().get("support");
        List<StorageClass> list = new ArrayList<>();
        boolean namespaced = false;

        // 取出存储配额
        Map<String, List<String>> rqMap;
        if (StringUtils.isNotBlank(namespace)) {
            namespaced = true;
            rqMap = resourceQuotaService.get(clusterId, namespace, namespace + "quota");
        } else {
            rqMap = resourceQuotaService.statistics(clusterId);
        }

        for (io.fabric8.kubernetes.api.model.storage.StorageClass sc : scList) {
            StorageClassProvisionerEnum provisionerEnum =
                StorageClassProvisionerEnum.findByProvisioner(sc.getProvisioner());
            if (provisionerEnum == null
                || namespaced && rqMap.size() > 0 && rqMap.get(sc.getMetadata().getName()) == null) {
                continue;
            }
            if (onlyMiddleware && !support.containsKey(provisionerEnum.getType())) {
                continue;
            }

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

}
