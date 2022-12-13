package com.middleware.zeus.service.k8s.impl;

import static com.middleware.caas.common.constants.NameConstant.CPU;
import static com.middleware.caas.common.constants.NameConstant.DISK;
import static com.middleware.caas.common.constants.NameConstant.MEMORY;
import static com.middleware.caas.common.constants.NameConstant.STORAGE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.middleware.zeus.service.k8s.ResourceQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.middleware.caas.common.enums.middleware.ResourceUnitEnum;
import com.middleware.caas.common.model.middleware.ResourceQuotaDTO;
import com.middleware.zeus.integration.cluster.ResourceQuotaWrapper;
import com.middleware.tool.numeric.ResourceCalculationUtil;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceQuota;

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
            (ns, rqList) -> dtoList.add(new ResourceQuotaDTO().setNamespace(ns).setQuotas(calculateQuotaList(rqList))));
        return dtoList;
    }

    @Override
    public Map<String, List<String>> statistics(String clusterId) {
        List<ResourceQuota> list = resourceQuotaWrapper.list(clusterId);
        return calculateQuotaList(list);
    }

    @Override
    public Map<String, List<String>> list(String clusterId, String namespace) {
        List<ResourceQuota> list = resourceQuotaWrapper.list(clusterId, namespace);
        return calculateQuotaList(list);
    }

    @Override
    public Map<String, List<String>> get(String clusterId, String namespace, String name) {
        ResourceQuota resourceQuota = resourceQuotaWrapper.get(clusterId, namespace, name);
        if (resourceQuota == null) {
            return new HashMap<>(0);
        }
        return calculateQuota(resourceQuota);
    }

    private Map<String, List<String>> calculateQuotaList(List<ResourceQuota> list) {
        Map<String, List<String>> rqMap = new HashMap<>();
        list.forEach(rq -> {
            Map<String, List<String>> map = calculateQuota(rq);
            map.forEach((k, v) -> {
                List<String> quotas = rqMap.computeIfAbsent(k, f -> new ArrayList<>());
                if (quotas.size() == 0) {
                    rqMap.put(k, v);
                } else {
                    quotas.set(1, String.valueOf(Double.parseDouble(quotas.get(1)) + Double.parseDouble(v.get(1))));
                    quotas.set(2, String.valueOf(Double.parseDouble(quotas.get(2)) + Double.parseDouble(v.get(2))));
                }
            });
        });
        return rqMap;
    }

    private Map<String, List<String>> calculateQuota(ResourceQuota resourceQuota) {
        Map<String, List<String>> rqMap = new HashMap<>();
        Map<String, Quantity> hard = resourceQuota.getSpec().getHard();
        Map<String, Quantity> used = resourceQuota.getStatus().getUsed();
        hard.forEach((k, v) -> {
            if (CPU.equals(k) || "requests.cpu".equals(k)) {
                double hardCpu = ResourceCalculationUtil.getResourceValue(v.toString(), CPU, "");
                double usedCpu = ResourceCalculationUtil.getResourceValue(used.get(k).toString(), CPU, "");
                // 总量，配额，使用量
                List<String> quota = Arrays.asList("0", String.valueOf(hardCpu), String.valueOf(usedCpu));
                rqMap.put(CPU, quota);
            } else if (MEMORY.equals(k) || "requests.memory".equals(k)) {
                double hardMemory =
                    ResourceCalculationUtil.getResourceValue(v.toString(), MEMORY, ResourceUnitEnum.GI.getUnit());
                double usedMemory = ResourceCalculationUtil.getResourceValue(used.get(k).toString(), MEMORY,
                    ResourceUnitEnum.GI.getUnit());
                // 总量，配额，使用量
                List<String> quota = Arrays.asList("0", String.valueOf(hardMemory), String.valueOf(usedMemory));
                rqMap.put(MEMORY, quota);
            } else if (k.endsWith(STORAGE)) {
                double hardStorage =
                    ResourceCalculationUtil.getResourceValue(v.toString(), DISK, ResourceUnitEnum.GI.getUnit());
                double usedStorage = ResourceCalculationUtil.getResourceValue(used.get(k).toString(), DISK,
                    ResourceUnitEnum.GI.getUnit());
                // 总量，配额，使用量
                List<String> quota = Arrays.asList("0", String.valueOf(hardStorage), String.valueOf(usedStorage));
                //String scName = k.substring(0, k.indexOf(".storageclass"));
                rqMap.put("storage", quota);
            }
        });
        return rqMap;
    }

}
