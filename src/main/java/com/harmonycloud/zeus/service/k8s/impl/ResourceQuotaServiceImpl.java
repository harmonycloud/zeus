package com.harmonycloud.zeus.service.k8s.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.CPU;
import static com.harmonycloud.caas.common.constants.NameConstant.DISK;
import static com.harmonycloud.caas.common.constants.NameConstant.MEMORY;
import static com.harmonycloud.caas.common.constants.NameConstant.STORAGE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.model.QuotaBase;
import com.harmonycloud.caas.common.model.ResourceQuotaDo;
import com.harmonycloud.caas.common.model.StorageQuota;
import com.harmonycloud.zeus.service.k8s.ResourceQuotaService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.harmonycloud.caas.common.enums.middleware.ResourceUnitEnum;
import com.harmonycloud.caas.common.model.middleware.ResourceQuotaDTO;
import com.harmonycloud.zeus.integration.cluster.ResourceQuotaWrapper;
import com.harmonycloud.tool.numeric.ResourceCalculationUtil;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import org.springframework.util.ObjectUtils;

/**
 * @author dengyulong
 * @date 2021/04/01
 */
@Service
public class ResourceQuotaServiceImpl implements ResourceQuotaService {

    @Autowired
    private ResourceQuotaWrapper resourceQuotaWrapper;

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
    public ResourceQuotaDo get(String clusterId, String namespace, String name) {
        ResourceQuota resourceQuota = resourceQuotaWrapper.get(clusterId, namespace, name);
        if (resourceQuota == null) {
            return new ResourceQuotaDo();
        }
        return calculateQuota(resourceQuota);
    }

    private ResourceQuotaDo calculateQuotaList(List<ResourceQuota> list) {
        //Map<String, List<String>> rqMap = new HashMap<>();
        ResourceQuotaDo resQuota = new ResourceQuotaDo();
        list.forEach(rq -> {
            ResourceQuotaDo quotaDo = calculateQuota(rq);
            // Map<String, List<String>> map = calculateQuota(rq);
            if (resQuota.isEmpty()) {
                BeanUtils.copyProperties(quotaDo, resQuota);
            } else {
                resQuota.setCpu(Double.compare(resQuota.getCpu().getRequest(), quotaDo.getCpu().getRequest()) < 0
                    ? resQuota.getCpu() : quotaDo.getCpu());
                resQuota
                    .setMemory(Double.compare(resQuota.getMemory().getRequest(), quotaDo.getMemory().getRequest()) < 0
                        ? resQuota.getMemory() : quotaDo.getMemory());
            }
            /*map.forEach((k, v) -> {
                List<String> quotas = rqMap.computeIfAbsent(k, f -> new ArrayList<>());
                if (quotas.size() == 0) {
                    rqMap.put(k, v);
                } else {
                    quotas.set(1, Double.compare(Double.parseDouble(quotas.get(1)), Double.parseDouble(v.get(1))) < 0
                        ? quotas.get(1) : v.get(1));
                    quotas.set(2, Double.compare(Double.parseDouble(quotas.get(2)), Double.parseDouble(v.get(2))) < 0
                        ? quotas.get(2) : v.get(2));
                }
            });*/
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
                // 总量，配额，使用量
                //List<String> quota = Arrays.asList("0", String.valueOf(hardCpu), String.valueOf(usedCpu));
                //rqMap.put(CPU, quota);
                QuotaBase cpu = new QuotaBase().setRequest(hardCpu).setUsed(usedCpu);
                quota.setCpu(cpu);
            } else if (MEMORY.equals(k) || "requests.memory".equals(k)) {
                double hardMemory =
                    ResourceCalculationUtil.getResourceValue(v.toString(), MEMORY, ResourceUnitEnum.GI.getUnit());
                double usedMemory = ResourceCalculationUtil.getResourceValue(used.get(k).toString(), MEMORY,
                    ResourceUnitEnum.GI.getUnit());
                // 总量，配额，使用量
                /*List<String> quota = Arrays.asList("0", String.valueOf(hardMemory), String.valueOf(usedMemory));
                rqMap.put(MEMORY, quota);*/
                QuotaBase memory = new QuotaBase().setRequest(hardMemory).setUsed(usedMemory);
                quota.setMemory(memory);
            } else if (k.endsWith(STORAGE)) {
                double hardStorage =
                    ResourceCalculationUtil.getResourceValue(v.toString(), DISK, ResourceUnitEnum.GI.getUnit());
                double usedStorage = ResourceCalculationUtil.getResourceValue(used.get(k).toString(), DISK,
                    ResourceUnitEnum.GI.getUnit());
                // 总量，配额，使用量
                //List<String> quota = Arrays.asList("0", String.valueOf(hardStorage), String.valueOf(usedStorage));
                //rqMap.put("storage", quota);
                String scName = k.substring(0, k.indexOf(".storageclass"));
                StorageQuota storageQuota = new StorageQuota().setName(scName).setStorage(new QuotaBase().setRequest(hardStorage).setUsed(usedStorage));
               quota.getStorageList().add(storageQuota);
            }
        });
        return quota;
    }

}
