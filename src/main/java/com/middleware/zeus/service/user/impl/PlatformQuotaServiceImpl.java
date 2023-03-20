package com.middleware.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.api.R;
import com.middleware.caas.common.model.QuotaBase;
import com.middleware.caas.common.model.ResourceQuotaDo;
import com.middleware.caas.common.model.StorageDto;
import com.middleware.caas.common.model.StorageQuota;
import com.middleware.caas.common.model.middleware.StorageClassInfo;
import com.middleware.zeus.bean.user.BeanPlatformQuota;
import com.middleware.zeus.dao.user.BeanPlatformQuotaMapper;
import com.middleware.zeus.service.k8s.StorageService;
import com.middleware.zeus.service.user.PlatformQuotaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.middleware.caas.common.constants.NameConstant.*;

/**
 * @author xutianhong
 * @Date 2023/3/7 4:38 下午
 */
@Service
@Slf4j
public class PlatformQuotaServiceImpl implements PlatformQuotaService {

    @Autowired
    private BeanPlatformQuotaMapper beanPlatformQuotaMapper;
    @Autowired
    private StorageService storageService;


    @Override
    public void allocate(String type, String uid, ResourceQuotaDo resourceQuotaDo) {

        String clusterId = resourceQuotaDo.getClusterId();
        // 分配 cpu
        if (resourceQuotaDo.getCpu() != null && resourceQuotaDo.getCpu().getRequest() != null) {
            remove(type, uid, CPU, CPU);
            this.insert(uid, type, clusterId, CPU, CPU, resourceQuotaDo.getCpu().getRequest());
        }
        // 分配memory
        if (resourceQuotaDo.getMemory() != null && resourceQuotaDo.getMemory().getRequest() != null) {
            remove(type, uid, MEMORY, MEMORY);
            this.insert(uid, type, clusterId, MEMORY, MEMORY, resourceQuotaDo.getMemory().getRequest());
        }

        // 分配存储
        if (!CollectionUtils.isEmpty(resourceQuotaDo.getStorageList())) {
            for (StorageQuota storageQuota : resourceQuotaDo.getStorageList()) {
                if ( storageQuota.getStorage() != null && storageQuota.getStorage().getRequest() != null) {
                    remove(type, uid, storageQuota.getStorageId(), STORAGE);
                    this.insert(uid, type, clusterId, STORAGE, storageQuota.getStorageId(),
                        storageQuota.getStorage().getRequest());
                }
            }
        }
    }

    @Override
    public void remove(String type, String uid, String name, String... target) {
        QueryWrapper<BeanPlatformQuota> wrapper = new QueryWrapper<BeanPlatformQuota>().eq("type", type);
        convertWrapper(wrapper, target);
        if (StringUtils.isNotEmpty(uid)){
            wrapper.eq("uid", uid);
        }
        if (StringUtils.isNotEmpty(name)){
            wrapper.eq("name", name);
        }
        beanPlatformQuotaMapper.delete(wrapper);
    }

    @Override
    public List<ResourceQuotaDo> getQuota(String type, String uid, String clusterId, String... target) {
        // 获取数据库配额使用情况
        QueryWrapper<BeanPlatformQuota> wrapper = new QueryWrapper<BeanPlatformQuota>().eq("type", type).eq("uid", uid);
        if (StringUtils.isNotEmpty(clusterId)){
            wrapper.eq("cluster_id", clusterId);
        }
        convertWrapper(wrapper, target);
        List<BeanPlatformQuota> list = beanPlatformQuotaMapper.selectList(wrapper);
        // 根据集群id装换为map
        Map<String, List<BeanPlatformQuota>> map = list.stream().collect(Collectors.groupingBy(BeanPlatformQuota::getClusterId));

        List<ResourceQuotaDo> quotaDoList = new ArrayList<>();
        for (String key : map.keySet()){
            ResourceQuotaDo resourceQuotaDo = new ResourceQuotaDo();
            resourceQuotaDo.setClusterId(key);
            for (BeanPlatformQuota platformQuota : map.get(key)){
                double quota = platformQuota.getQuota();
                switch (platformQuota.getTarget()){
                    case CPU:
                        QuotaBase cpu = new QuotaBase();
                        cpu.setRequest(quota);
                        resourceQuotaDo.setCpu(cpu);
                        break;
                    case MEMORY:
                        QuotaBase memory = new QuotaBase();
                        memory.setRequest(quota);
                        resourceQuotaDo.setMemory(memory);
                        break;
                    case STORAGE:
                        StorageQuota storageQuota = new StorageQuota();
                        storageQuota.setStorage(new QuotaBase().setRequest(quota));
                        storageQuota.setStorageId(platformQuota.getName());
                        resourceQuotaDo.getStorageList().add(storageQuota);
                        break;
                    default:
                }
            }
            quotaDoList.add(resourceQuotaDo);
        }
        return quotaDoList;
    }

    @Override
    public List<ResourceQuotaDo> getQuota(String type, List<String> uidList, String... target) {
        // 获取配额使用情况
        QueryWrapper<BeanPlatformQuota> wrapper = new QueryWrapper<BeanPlatformQuota>().eq("type", type);
        convertWrapper(wrapper, target);
        List<BeanPlatformQuota> list = beanPlatformQuotaMapper.selectList(wrapper);
        // 根据uid list过滤
        if (!CollectionUtils.isEmpty(uidList)) {
            list = list.stream()
                .filter(beanPlatformQuota -> uidList.stream().anyMatch(uid -> uid.equals(beanPlatformQuota.getUid())))
                .collect(Collectors.toList());
        }
        // 根据集群id装换为map
        Map<String, List<BeanPlatformQuota>> map =
            list.stream().collect(Collectors.groupingBy(BeanPlatformQuota::getClusterId));

        // 封装数据，合并同一集群下的配额
        List<ResourceQuotaDo> quotaDoList = new ArrayList<>();
        for (String key : map.keySet()) {
            ResourceQuotaDo resourceQuotaDo = new ResourceQuotaDo();
            resourceQuotaDo.setClusterId(key);

            double cpu = 0.0;
            double memory = 0.0;
            Map<String, Double> storageMap = new HashMap<>();
            for (BeanPlatformQuota platformQuota : map.get(key)) {
                double quota = platformQuota.getQuota();
                switch (platformQuota.getTarget()) {
                    case CPU:
                        cpu += quota;
                        break;
                    case MEMORY:
                        memory += quota;
                        break;
                    case STORAGE:
                        if (storageMap.containsKey(platformQuota.getName())) {
                            double temp = storageMap.get(platformQuota.getName());
                            storageMap.put(platformQuota.getName(), temp + quota);
                        } else {
                            storageMap.put(platformQuota.getName(), quota);
                        }
                        break;
                    default:
                }
            }
            resourceQuotaDo.setCpu(new QuotaBase().setRequest(cpu));
            resourceQuotaDo.setMemory(new QuotaBase().setRequest(memory));
            for (String storage : storageMap.keySet()) {
                StorageQuota storageQuota = new StorageQuota();
                storageQuota.setStorage(new QuotaBase().setRequest(storageMap.get(storage)));
                storageQuota.setStorageId(storage);
            }
            quotaDoList.add(resourceQuotaDo);
        }
        return quotaDoList;
    }

    @Override
    public List<BeanPlatformQuota> findQuota(String type, List<String> uidList, String clusterId, String name,
        String... target) {
        QueryWrapper<BeanPlatformQuota> wrapper =
            new QueryWrapper<BeanPlatformQuota>().eq("type", type).eq("cluster_id", clusterId);
        convertWrapper(wrapper, target);
        if (StringUtils.isNotEmpty(name)) {
            wrapper.eq("name", name);
        }
        List<BeanPlatformQuota> list = beanPlatformQuotaMapper.selectList(wrapper);
        // 根据uid list过滤
        if (!CollectionUtils.isEmpty(uidList)) {
            list = list.stream()
                .filter(beanPlatformQuota -> uidList.stream().anyMatch(uid -> uid.equals(beanPlatformQuota.getUid())))
                .collect(Collectors.toList());
        }
        return list;
    }

    @Override
    public List<ResourceQuotaDo> convertUsedResource(List<ResourceQuotaDo> a1, List<ResourceQuotaDo> a2) {
        // 根据集群id转换为map
        Map<String, ResourceQuotaDo> a2Map =
            a2.stream().collect(Collectors.toMap(ResourceQuotaDo::getClusterId, Function.identity()));

        for (ResourceQuotaDo quotaDo : a1) {
            if (a2Map.containsKey(quotaDo.getClusterId())) {
                // 设置cpu memory 使用量
                ResourceQuotaDo projectQuotaDo = a2Map.get(quotaDo.getClusterId());
                quotaDo.getCpu().setUsed(projectQuotaDo.getCpu().getRequest());
                quotaDo.getMemory().setUsed(projectQuotaDo.getMemory().getRequest());

                // 设置存储使用量
                if (!CollectionUtils.isEmpty(quotaDo.getStorageList())
                    && !CollectionUtils.isEmpty(projectQuotaDo.getStorageList())) {
                    Map<List<String>,
                        Double> storageMap = projectQuotaDo.getStorageList().stream()
                            .collect(Collectors.toMap(
                                storageQuota -> storageQuota.getStorageClass().stream().sorted()
                                    .collect(Collectors.toList()),
                                storageQuota -> storageQuota.getStorage().getRequest()));
                    for (StorageQuota storageQuota : quotaDo.getStorageList()) {
                        List<String> storageClassList =
                            storageQuota.getStorageClass().stream().sorted().collect(Collectors.toList());
                        if (storageMap.containsKey(storageClassList)) {
                            storageQuota.getStorage().setUsed(storageMap.get(storageClassList));
                        }
                    }
                }
            }
        }
        return a1;
    }

    @Override
    public void convertStorageName(List<ResourceQuotaDo> resourceQuotaDoList) {
        // 获取存储名称
        for (ResourceQuotaDo quotaDo : resourceQuotaDoList) {
            // 封装获取包含storageClass 和 对应别名的map

            List<StorageDto> storageDtoList = storageService.list(quotaDo.getClusterId(), false);
            Map<String, String> storageNameMap =
                storageDtoList.stream().collect(Collectors.toMap(StorageDto::getStorageId, StorageDto::getAliasName));
            Map<String, List<String>> storageTypeMap =
                storageDtoList.stream().collect(Collectors.toMap(StorageDto::getStorageId, storageDto -> storageDto
                    .getStorageClassList().stream().map(StorageClassInfo::getVolumeType).collect(Collectors.toList())));
            // 设置存储名称
            for (StorageQuota storageQuota : quotaDo.getStorageList()) {
                if (storageNameMap.containsKey(storageQuota.getStorageId())) {
                    storageQuota.setName(storageNameMap.get(storageQuota.getStorageId()));
                    storageQuota.setStorageType(storageTypeMap.get(storageQuota.getStorageId()));
                }
            }
        }
    }

    private void convertWrapper(QueryWrapper<BeanPlatformQuota> wrapper, String... target){
        if (target.length > 0) {
            wrapper.and(wq -> {
                for (int i = 0; i < target.length; ++i) {
                    wq.eq("target", target[i]);
                    if (i != target.length - 1) {
                        wq.or();
                    }
                }
            });
        }
    }
    
    private void insert(String uid, String type, String clusterId, String target, String name, Double quota){
        BeanPlatformQuota beanPlatformQuota = new BeanPlatformQuota();
        beanPlatformQuota.setUid(uid);
        beanPlatformQuota.setType(type);
        beanPlatformQuota.setClusterId(clusterId);
        beanPlatformQuota.setTarget(target);
        beanPlatformQuota.setName(name);
        beanPlatformQuota.setQuota(quota);
        beanPlatformQuotaMapper.insert(beanPlatformQuota);
    }


}
