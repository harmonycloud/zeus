package com.middleware.zeus.service.k8s.impl;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.middleware.zeus.common.model.QuotaBase;
import com.middleware.zeus.common.model.ResourceQuotaDo;
import com.middleware.zeus.common.model.StorageDto;
import com.middleware.zeus.common.model.StorageQuota;
import com.middleware.zeus.common.model.middleware.StorageClassInfo;
import com.middleware.zeus.service.k8s.ResourceQuotaService;
import com.middleware.zeus.service.k8s.StorageService;
import com.middleware.zeus.util.numeric.CalculateUtil;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ResourceQuotaSpec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.common.enums.middleware.ResourceUnitEnum;
import com.middleware.zeus.common.model.middleware.ResourceQuotaDTO;
import com.middleware.zeus.integration.cluster.ResourceQuotaWrapper;
import com.middleware.zeus.util.numeric.ResourceCalculationUtil;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceQuota;

import static com.middleware.zeus.common.constants.CommonConstant.DOT;
import static com.middleware.zeus.common.constants.NameConstant.*;

/**
 * @author dengyulong
 * @date 2021/04/01
 */
@Service
public class ResourceQuotaServiceImpl implements ResourceQuotaService {

    @Autowired
    private ResourceQuotaWrapper resourceQuotaWrapper;
    @Autowired
    private StorageService storageService;

    @Override
    public void create(String clusterId, String namespace, ResourceQuotaDo resourceQuotaDo) {

        ResourceQuota resourceQuota = new ResourceQuota();

        ObjectMeta meta = new ObjectMeta();
        meta.setName(getName(namespace));
        meta.setNamespace(namespace);

        // 封装资源配额数据
        ResourceQuotaSpec spec = convertDoToResourceQuotaSpec(resourceQuotaDo);

        resourceQuota.setMetadata(meta);
        resourceQuota.setSpec(spec);
        resourceQuotaWrapper.createOrReplace(clusterId, namespace, resourceQuota);
    }

    @Override
    public void update(String clusterId, String namespace, ResourceQuotaDo resourceQuotaDo) {
        ResourceQuota resourceQuota = resourceQuotaWrapper.get(clusterId, namespace, getName(namespace));
        if (resourceQuota == null){
            create(clusterId, namespace, resourceQuotaDo);
            return;
        }
        // 封装资源配额数据
        ResourceQuotaSpec spec = convertDoToResourceQuotaSpec(resourceQuotaDo);
        resourceQuota.setSpec(spec);
        resourceQuotaWrapper.createOrReplace(clusterId, namespace, resourceQuota);
    }

    @Override
    public List<ResourceQuotaDTO> list(String clusterId) {
        List<ResourceQuota> list = resourceQuotaWrapper.list(clusterId);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        // 按命名空间转成map
        Map<String, List<ResourceQuota>> nsRqMap =
                list.stream().collect(Collectors.groupingBy(rq -> rq.getMetadata().getNamespace()));
        // 组装返回值
        List<ResourceQuotaDTO> dtoList = new ArrayList<>(nsRqMap.size());
        nsRqMap.forEach(
                (ns, rqList) -> dtoList.add(new ResourceQuotaDTO().setNamespace(ns).setResourceQuotaDo(calculateQuotaList(rqList))));
        return dtoList;
    }

    @Override
    public ResourceQuotaDo statistics(String clusterId) {
        List<ResourceQuota> list = resourceQuotaWrapper.list(clusterId);
        return calculateQuotaList(list);
    }

    @Override
    public ResourceQuotaDo list(String clusterId, String namespace) {
        List<ResourceQuota> list = resourceQuotaWrapper.list(clusterId, namespace);
        return calculateQuotaList(list);
    }

    @Override
    public ResourceQuotaDo list(String clusterId, String namespace, String storageClass) {
        ResourceQuotaDo resourceQuotaDo = list(clusterId, namespace);
        if (StringUtils.isNotEmpty(storageClass) && !CollectionUtils.isEmpty(resourceQuotaDo.getStorageList())) {
            resourceQuotaDo.setStorageList(resourceQuotaDo.getStorageList().stream()
                    .filter(storageQuota -> storageQuota.getName().equals(storageClass)).collect(Collectors.toList()));
        }
        return resourceQuotaDo;
    }

    @Override
    public ResourceQuotaDo get(String clusterId, String namespace, String name) {
        ResourceQuota resourceQuota = resourceQuotaWrapper.get(clusterId, namespace, name);
        if (resourceQuota == null) {
            return new ResourceQuotaDo();
        }
        return calculateQuota(resourceQuota);
    }

    @Override
    public ResourceQuotaDo calculateQuota(List<ResourceQuotaDo> resourceQuotaDoList) {
        double cpu = 0.0;
        double usedCpu = 0.0;
        double memory = 0.0;
        double usedMemory = 0.0;
        Map<String, Double> storageMap = new HashMap<>();
        Map<String, Double> usedStorageMap = new HashMap<>();
        Map<String, String> storageIdMap = new HashMap<>();
        for (ResourceQuotaDo quota : resourceQuotaDoList){
            if (quota != null){
                if (quota.getCpu() != null && quota.getCpu().getRequest() != null){
                    cpu += quota.getCpu().getRequest();
                }
                if (quota.getMemory() != null && quota.getMemory().getRequest() != null){
                    memory += quota.getMemory().getRequest();
                }
                if (quota.getCpu() != null && quota.getCpu().getUsed() != null){
                    usedCpu += quota.getCpu().getUsed();
                }
                if (quota.getMemory() != null && quota.getMemory().getUsed() != null){
                    usedMemory += quota.getMemory().getUsed();
                }
                // 获取已分配storage
                if (!CollectionUtils.isEmpty(quota.getStorageList())){
                    for (StorageQuota storageQuota : quota.getStorageList()){
                        // 设置request
                        if (storageMap.containsKey(storageQuota.getName())){
                            Double request = storageMap.get(storageQuota.getName());
                            storageMap.put(storageQuota.getName(), request + storageQuota.getStorage().getRequest());
                        }else {
                            storageMap.put(storageQuota.getName(), storageQuota.getStorage().getRequest());
                        }
                        // 设置used
                        if (usedStorageMap.containsKey(storageQuota.getName())){
                            Double used = usedStorageMap.get(storageQuota.getName());
                            usedStorageMap.put(storageQuota.getName(), used + storageQuota.getStorage().getUsed());
                        }else {
                            usedStorageMap.put(storageQuota.getName(), storageQuota.getStorage().getUsed());
                        }
                        
                        storageIdMap.put(storageQuota.getName(), storageQuota.getStorageId());
                    }
                }
            }
        }
        ResourceQuotaDo resourceQuotaDo = new ResourceQuotaDo();
        resourceQuotaDo.getCpu().setRequest(cpu);
        resourceQuotaDo.getMemory().setRequest(memory);
        resourceQuotaDo.getCpu().setUsed(usedCpu);
        resourceQuotaDo.getMemory().setUsed(usedMemory);
        // 设置storage quota
        List<StorageQuota> storageList = new ArrayList<>();
        for (String key : storageMap.keySet()){
            StorageQuota storageQuota = new StorageQuota();
            storageQuota.setName(key);
            storageQuota.setStorageId(storageIdMap.get(key));
            QuotaBase storage = new QuotaBase();
            storage.setRequest(storageMap.get(key));
            storage.setUsed(usedStorageMap.get(key));
            storageQuota.setStorage(storage);
            storageList.add(storageQuota);
        }
        resourceQuotaDo.setStorageList(storageList);

        return resourceQuotaDo;
    }

    private ResourceQuotaDo calculateQuotaList(List<ResourceQuota> list) {
        ResourceQuotaDo resQuota = new ResourceQuotaDo();
        list.forEach(rq -> {
            ResourceQuotaDo quotaDo = calculateQuota(rq);
            if (resQuota.isEmpty()) {
                BeanUtils.copyProperties(quotaDo, resQuota);
            } else {
                resQuota.setCpu(Double.compare(resQuota.getCpu().getRequest(), quotaDo.getCpu().getRequest()) < 0
                        ? resQuota.getCpu() : quotaDo.getCpu());
                resQuota
                        .setMemory(Double.compare(resQuota.getMemory().getRequest(), quotaDo.getMemory().getRequest()) < 0
                                ? resQuota.getMemory() : quotaDo.getMemory());
            }
        });
        return resQuota;
    }

    private ResourceQuotaDo calculateQuota(ResourceQuota resourceQuota) {
        //Map<String, List<String>> rqMap = new HashMap<>();
        ResourceQuotaDo quota = new ResourceQuotaDo().setStorageList(new ArrayList<>());
        Map<String, Quantity> hard = resourceQuota.getSpec().getHard();
        Map<String, Quantity> used = resourceQuota.getStatus().getUsed();
        hard.forEach((k, v) -> {
            if (CPU.equals(k) || "requests.cpu".equals(k)) {
                double hardCpu = ResourceCalculationUtil.getResourceValue(v.toString(), CPU, "");
                double usedCpu = ResourceCalculationUtil.getResourceValue(used.get(k).toString(), CPU, "");
                QuotaBase cpu = new QuotaBase().setRequest(hardCpu).setUsed(usedCpu);
                if (hardCpu != 0){
                    cpu.setUsage(CalculateUtil.division(usedCpu, hardCpu, 4) * 100);
                }
                quota.setCpu(cpu);
            } else if (MEMORY.equals(k) || "requests.memory".equals(k)) {
                double hardMemory =
                        ResourceCalculationUtil.getResourceValue(v.toString(), MEMORY, ResourceUnitEnum.GI.getUnit());
                double usedMemory = ResourceCalculationUtil.getResourceValue(used.get(k).toString(), MEMORY,
                        ResourceUnitEnum.GI.getUnit());
                QuotaBase memory = new QuotaBase().setRequest(hardMemory).setUsed(usedMemory);
                if(hardMemory != 0){
                    memory.setUsage(CalculateUtil.division(usedMemory, hardMemory, 4) * 100);
                }
                quota.setMemory(memory);
            } else if (k.endsWith(STORAGE_CLASS_STORAGE_K8s_IO_REQUESTS_STORAGE)) {
                double hardStorage =
                        ResourceCalculationUtil.getResourceValue(v.toString(), DISK, ResourceUnitEnum.GI.getUnit());
                double usedStorage = ResourceCalculationUtil.getResourceValue(used.get(k).toString(), DISK,
                        ResourceUnitEnum.GI.getUnit(), 0, RoundingMode.UP);
                String scName = k.substring(0, k.indexOf(".storageclass"));
                StorageQuota storageQuota = new StorageQuota().setName(scName).setStorage(new QuotaBase().setRequest(hardStorage).setUsed(usedStorage));
                quota.getStorageList().add(storageQuota);
            }
        });
        return quota;
    }

    public ResourceQuotaSpec convertDoToResourceQuotaSpec(ResourceQuotaDo resourceQuotaDo){
        ResourceQuotaSpec spec = new ResourceQuotaSpec();
        Map<String, Quantity> hard = new HashMap<>();
        // 设置cpu配额
        if (resourceQuotaDo.getCpu() != null && resourceQuotaDo.getCpu().getRequest() != null){
            Quantity quantity = new Quantity();
            quantity.setAmount(String.valueOf(resourceQuotaDo.getCpu().getRequest()));
            hard.put(CPU, quantity);
        }
        // 设置memory配额
        if (resourceQuotaDo.getMemory() != null && resourceQuotaDo.getMemory().getRequest() != null){
            Quantity quantity = new Quantity();
            quantity.setAmount(resourceQuotaDo.getMemory().getRequest() + ResourceUnitEnum.GI.getUnit());
            hard.put(MEMORY, quantity);
        }
        // 设置storage配额
        if (!CollectionUtils.isEmpty(resourceQuotaDo.getStorageList())) {
            // 查询存储列表  并根据存储id转换为map
            List<StorageDto> storageDtoList = storageService.list(resourceQuotaDo.getClusterId(), false);
            Map<String, List<String>> storageClassListMap =
                storageDtoList.stream().collect(Collectors.toMap(StorageDto::getStorageId, storageDto -> storageDto
                    .getStorageClassList().stream().map(StorageClassInfo::getName).collect(Collectors.toList())));
            // 封装存储数据
            for (StorageQuota storageQuota : resourceQuotaDo.getStorageList()) {
                // 获取storageList
                if (storageClassListMap.containsKey(storageQuota.getStorageId())){
                    storageQuota.setStorageClass(storageClassListMap.get(storageQuota.getStorageId()));
                }
                if (!CollectionUtils.isEmpty(storageQuota.getStorageClass()) && storageQuota.getStorage() != null
                        && storageQuota.getStorage().getRequest() != null) {
                    for (String storageClass : storageQuota.getStorageClass()) {
                        Quantity quantity = new Quantity();
                        quantity.setAmount(storageQuota.getStorage().getRequest() + ResourceUnitEnum.GI.getUnit());
                        hard.put(storageClass + DOT + STORAGE_CLASS_STORAGE_K8s_IO_REQUESTS_STORAGE, quantity);
                    }
                }
            }
        }

        spec.setHard(hard);
        return spec;
    }

    public String getName(String namespace){
        return namespace + QUOTA;
    }

}
