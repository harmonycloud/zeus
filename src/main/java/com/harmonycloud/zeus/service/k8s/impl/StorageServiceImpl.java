package com.harmonycloud.zeus.service.k8s.impl;

import static com.harmonycloud.caas.common.constants.CommonConstant.*;
import static com.harmonycloud.caas.common.constants.NameConstant.VG_NAME;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.StorageClassProvisionerEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.MonitorResourceQuota;
import com.harmonycloud.caas.common.model.PersistentVolumeClaim;
import com.harmonycloud.caas.common.model.StorageDto;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.zeus.integration.cluster.StorageClassWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.middleware.MiddlewareCrTypeService;
import com.harmonycloud.zeus.service.prometheus.PrometheusResourceMonitorService;
import com.harmonycloud.zeus.service.registry.HelmChartService;

import io.fabric8.kubernetes.api.model.storage.StorageClass;
import lombok.extern.slf4j.Slf4j;

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
    @Autowired
    private MiddlewareCRService middlewareCRService;
    @Autowired
    private PodService podService;
    @Autowired
    private MiddlewareCrTypeService middlewareCrTypeService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private ClusterService clusterService;

    @Override
    public List<String> getType() {
        return Arrays.stream(StorageClassProvisionerEnum.values()).map(StorageClassProvisionerEnum::getType)
            .collect(Collectors.toList());
    }

    @Override
    public List<StorageDto> list(String clusterId, String key, String type, Boolean all) {
        List<MiddlewareClusterDTO> clusterList = new ArrayList<>();
        if (clusterId.equals(ASTERISK)) {
            clusterList = clusterService.listClusters();
        } else {
            clusterList.add(clusterService.findById(clusterId));
        }
        List<StorageDto> result = new ArrayList<>();
        for (MiddlewareClusterDTO cluster : clusterList) {
            List<StorageClass> storageClassList = storageClassWrapper.list(cluster.getId());
            List<StorageDto> storageDtoList = storageClassList.stream()
                .filter(storageClass -> all
                    || storageClass.getMetadata() != null && storageClass.getMetadata().getLabels() != null
                        && storageClass.getMetadata().getLabels().containsKey(MIDDLEWARE))
                .map(storageClass -> {
                    // 初始化业务对象
                    return convert(cluster.getId(), storageClass);
                }).filter(storageDto -> {
                    if (StringUtils.isNotEmpty(key)) {
                        return storageDto.getAliasName().contains(key) || storageDto.getName().contains(key);
                    }
                    return true;
                }).filter(storageDto -> {
                    if (StringUtils.isNotEmpty(type)) {
                        return storageDto.getVolumeType().equals(type);
                    }
                    return true;
                }).collect(Collectors.toList());
            result.addAll(storageDtoList);
        }
        return result;
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
        List<StorageClass> storageClassList = storageClassWrapper.list(storageDto.getClusterId(), labels);
        if (!CollectionUtils.isEmpty(storageClassList)){
            throw new BusinessException(ErrorMessage.STORAGE_CLASS_NAME_EXIST);
        }
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
        List<StorageClass> storageClassList = storageClassWrapper.list(storageDto.getClusterId(), labels);
        if (!CollectionUtils.isEmpty(storageClassList)){
            throw new BusinessException(ErrorMessage.STORAGE_CLASS_NAME_EXIST);
        }
        storageClassWrapper.update(storageDto.getClusterId(), storageClass);
    }

    @Override
    public StorageDto detail(String clusterId, String storageName) {
        StorageClass storageClass = storageClassWrapper.get(clusterId, storageName);
        StorageDto storageDto = convert(clusterId, storageClass);

        // 查询存储
        List<PersistentVolumeClaim> pvcList = pvcService.list(clusterId, null);
        pvcList = pvcList.stream().filter(pvc -> pvc.getStorageClassName().equals(storageName)).collect(Collectors.toList());


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
    public List<Middleware> middlewares(String clusterId, String storageName) {
        // 获取所有中间件cr
        List<MiddlewareCR> middlewareCRList = middlewareCRService.listCR(clusterId, null, null);

        // 获取使用了该存储的pvc
        Map<String, String> fields = new HashMap<>();
        fields.put("spec.storageClassName", storageName);
        List<PersistentVolumeClaim> pvcList = pvcService.listWithFields(clusterId, null, fields);

        // 过滤获取到使用了该存储的中间件
        middlewareCRList = middlewareCRList.stream().filter(middlewareCr -> {
            Map<String, List<MiddlewareInfo>> include = middlewareCr.getStatus().getInclude();
            if (CollectionUtils.isEmpty(include) || !include.containsKey(PERSISTENT_VOLUME_CLAIMS)) {
                return false;
            }
            List<MiddlewareInfo> middlewareInfoList = include.get(PERSISTENT_VOLUME_CLAIMS);
            return pvcList.stream().anyMatch(pvc -> middlewareInfoList.stream()
                .anyMatch(middlewareInfo -> middlewareInfo.getName().equals(pvc.getName())));
        }).collect(Collectors.toList());

        List<Middleware> middlewareList = new ArrayList<>();
        for (MiddlewareCR middlewareCr : middlewareCRList){
            // 获取pod列表
            Middleware middleware = podService.list(clusterId, middlewareCr.getMetadata().getNamespace(),
                middlewareCr.getMetadata().getName(),
                middlewareCrTypeService.findTypeByCrType(middlewareCr.getSpec().getType()));
            // 转换创建时间
            middleware.setCreateTime(DateUtils.parseUTCDate(middlewareCr.getMetadata().getCreationTimestamp()));

            List<MiddlewareInfo> pvcNameList = middlewareCr.getStatus().getInclude().get(PERSISTENT_VOLUME_CLAIMS);

            StringBuilder pvcs = new StringBuilder();
            pvcNameList.forEach(pvcName -> pvcs.append(pvcName).append("|"));

            // 查询storage request
            String totalStorageQuery =
                    "sum(kube_persistentvolumeclaim_resource_requests_storage_bytes{persistentvolumeclaim=~\""
                            + pvcs.toString() + "\",namespace=\"" + middlewareCr.getMetadata().getNamespace()
                            + "\"}) by (persistentvolumeclaim) /1024/1024/1024";
            Map<String, Double> totalResult = prometheusResourceMonitorService.queryPvcs(clusterId, totalStorageQuery);

            // 查询storage using
            String usedStorageQuery =
                    "sum(kubelet_volume_stats_used_bytes{persistentvolumeclaim=~\""
                            + pvcs.toString() + "\",namespace=\"" + middlewareCr.getMetadata().getNamespace()
                            + "\",endpoint!=\"\"}) by (persistentvolumeclaim) /1024/1024/1024";
            Map<String, Double> usingResult = prometheusResourceMonitorService.queryPvcs(clusterId, usedStorageQuery);

            // 封装数据
            // 计算该中间件总存储
            double totalStorage = 0.0;
            double usedStorage = 0.0;
            for (PodInfo pod : middleware.getPods()) {
                MonitorResourceQuota podQuota = new MonitorResourceQuota();
                String num = pod.getPodName().substring(pod.getPodName().length() - 1);
                podQuota.getStorage().setTotal(totalResult.get(num));
                podQuota.getStorage().setUsed(usingResult.get(num));
                // calculate
                totalStorage = totalStorage + totalResult.get(num);
                usedStorage = usedStorage + usingResult.get(num);

                pod.setMonitorResourceQuota(podQuota);
            }

            MonitorResourceQuota middlewareQuota = new MonitorResourceQuota();
            middlewareQuota.getStorage().setTotal(totalStorage);
            middlewareQuota.getStorage().setUsed(usedStorage);

            middleware.setMonitorResourceQuota(middlewareQuota);
            JSONObject values = helmChartService.getInstalledValues(middleware, clusterService.findById(clusterId));
            if (values.containsKey("chart-version")){
                middleware.setImagePath(middleware.getType() + "-" + values.getString("chart-version") + ".svg");
            }
            middleware.setAliasName(values.getOrDefault("aliasName", null).toString());
        }

        return middlewareList;
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
            Double total = Double.parseDouble(Pattern.compile("[^0-9]").matcher(annotations.get(STORAGE_LIMIT)).replaceAll("").trim());
            monitorResourceQuota.getStorage().setTotal(total);
            storageDto.setMonitorResourceQuota(monitorResourceQuota);
        }
        // 获取中文名称
        Map<String, String> labels = storageClass.getMetadata().getLabels();
        if (labels != null && labels.containsKey(ALIAS_NAME)){
            storageDto.setAliasName(labels.get(ALIAS_NAME));
        }
        // 获取vg_name
        if (storageClass.getParameters() != null && storageClass.getParameters().containsKey(VG_NAME)){
            storageDto.setVgName(storageClass.getParameters().get(VG_NAME));
        }

        storageDto.setClusterId(clusterId);
        storageDto.setName(storageClass.getMetadata().getName());
        storageDto.setProvisioner(storageClass.getProvisioner());

        // 获取类型
        String type = StorageClassProvisionerEnum.findByProvisioner(storageClass.getProvisioner()).getType();
        storageDto.setVolumeType(type == null ? "unknown" : type);

        return storageDto;
    }

}
