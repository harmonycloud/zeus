package com.harmonycloud.zeus.service.k8s.impl;

import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.ResourceUnitEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.MonitorResourceQuota;
import com.harmonycloud.caas.common.model.PersistentVolumeClaim;
import com.harmonycloud.caas.common.model.StorageClassDTO;
import com.harmonycloud.caas.common.model.StorageDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareResourceInfo;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.tool.numeric.ResourceCalculationUtil;
import com.harmonycloud.zeus.integration.cluster.StorageClassWrapper;
import com.harmonycloud.zeus.service.k8s.PvcService;
import com.harmonycloud.zeus.service.k8s.StorageService;
import com.harmonycloud.zeus.service.prometheus.PrometheusResourceMonitorService;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.CommonConstant.ALIAS_NAME;
import static com.harmonycloud.caas.common.constants.CommonConstant.TRUE;
import static com.harmonycloud.caas.common.constants.NameConstant.MEMORY;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.STORAGE_LIMIT;

/**
 * @author xutianhong
 * @Date 2022/6/8 10:29 上午
 */
@Service
@Slf4j
public class StorageServiceImpl implements StorageService {

    @Autowired
    private StorageClassWrapper storageClassWrapper;
    @Autowired
    private PvcService pvcService;
    @Autowired
    private PrometheusResourceMonitorService prometheusResourceMonitorService;


    @Override
    public List<StorageDto> list(String clusterId, String key, String type, Boolean all) {
        List<StorageClass> storageClassList = storageClassWrapper.list(clusterId);
        return storageClassList.stream()
            .filter(storageClass -> all || storageClass.getMetadata() != null
                && storageClass.getMetadata().getLabels() != null
                && storageClass.getMetadata().getLabels().containsKey(MIDDLEWARE))
            .map(storageClass -> {
                // 初始化业务对象
                StorageDto storageDto = convert(clusterId, storageClass);
                /*if (!all){
                    Map<String, String> fields = new HashMap<>();
                    fields.put("storageClassName", storageClass.getMetadata().getName());
                    pvcService.listWithFields(clusterId, null, fields);
                }*/
                return storageDto;
            }).collect(Collectors.toList());
    }

    @Override
    public void add(StorageDto storageDto) {
        StorageClass storageClass = storageClassWrapper.get(storageDto.getClusterId(), storageDto.getName());
        if (storageClass == null){
            throw new BusinessException(ErrorMessage.STORAGE_CLASS_NOT_FOUND);
        }
        Map<String, String> labels = storageClass.getMetadata().getLabels();
        if (labels == null){
            labels = new HashMap<>();
        }
        labels.put(MIDDLEWARE, TRUE);
        labels.put(ALIAS_NAME, storageDto.getAliasName());
        storageClassWrapper.update(storageDto.getClusterId(), storageClass);
    }

    @Override
    public void delete(String clusterId, String storageName) {
        StorageClass storageClass = storageClassWrapper.get(clusterId, storageName);
        if (storageClass == null){
            throw new BusinessException(ErrorMessage.STORAGE_CLASS_NOT_FOUND);
        }
        Map<String, String> labels = storageClass.getMetadata().getLabels();
        if (labels != null){
            labels.remove(MIDDLEWARE);
            labels.remove(ALIAS_NAME);
            storageClassWrapper.update(clusterId, storageClass);
        }
    }

    @Override
    public void update(StorageDto storageDto) {

    }

    @Override
    public StorageDto detail(String clusterId, String storageName) {
        StorageClass storageClass = storageClassWrapper.get(clusterId, storageName);
        StorageDto storageDto = convert(clusterId, storageClass);

        //todo 根据源获取类型

        // 查询存储
        Map<String, String> fields = new HashMap<>();
        fields.put("storageClassName", storageClass.getMetadata().getName());
        List<PersistentVolumeClaim> pvcList = pvcService.listWithFields(clusterId, null, fields);


        StringBuilder sb = new StringBuilder();
        for (PersistentVolumeClaim pvc : pvcList){
            sb.append(pvc.getName()).append("|");
        }
        // 查询申请配额
        String requestQuery = "sum(kube_persistentvolumeclaim_resource_requests_storage_bytes{persistentvolumeclaim=~\""
                + sb.toString() + "\"}) by (persistentvolumeclaim) /1024/1024/1024";
        double request = prometheusResourceMonitorService.queryAndConvert(clusterId, requestQuery);

        // 查询使用量
        String usedQuery = "sum(kubelet_volume_stats_used_bytes{persistentvolumeclaim=~\""
                + sb.toString() + "\",endpoint!=\"\"}) by (persistentvolumeclaim) /1024/1024/1024";
        double used = prometheusResourceMonitorService.queryAndConvert(clusterId, usedQuery);

        MonitorResourceQuota monitorResourceQuota = storageDto.getMonitorResourceQuota();
        if (monitorResourceQuota == null){
            monitorResourceQuota = new MonitorResourceQuota();
        }
        monitorResourceQuota.getStorage().setRequest(request);
        monitorResourceQuota.getStorage().setUsed(used);
        return storageDto;
    }

    @Override
    public List<MiddlewareResourceInfo> middlewares(String clusterId, String storageName) {
        return null;
    }

    @Override
    public List<PodInfo> pods(String clusterId, String storageName, String middlewareName) {
        return null;
    }

    /**
     * 封装业务对象
     */
    public StorageDto convert(String clusterId, StorageClass storageClass){
        // 初始化业务对象
        StorageDto storageDto = new StorageDto();
        // 获取存储配额
        Map<String, String> annotations =  storageClass.getMetadata().getAnnotations();
        if (annotations != null && annotations.containsKey(STORAGE_LIMIT)){
            MonitorResourceQuota monitorResourceQuota = new MonitorResourceQuota();
            Double total = ResourceCalculationUtil.getResourceValue(annotations.get(STORAGE_LIMIT), MEMORY, ResourceUnitEnum.GI.getUnit());
            monitorResourceQuota.getStorage().setTotal(total);
            storageDto.setMonitorResourceQuota(monitorResourceQuota);
        }
        // 获取中文名称
        Map<String, String> labels = storageClass.getMetadata().getLabels();
        if (labels != null && labels.containsKey(ALIAS_NAME)){
            storageDto.setAliasName(labels.get(ALIAS_NAME));
        }

        storageDto.setClusterId(clusterId);
        storageDto.setName(storageClass.getMetadata().getName());
        storageDto.setProvisioner(storageClass.getProvisioner());

        return storageDto;
    }

}
