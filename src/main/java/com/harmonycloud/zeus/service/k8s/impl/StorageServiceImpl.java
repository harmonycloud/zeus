package com.harmonycloud.zeus.service.k8s.impl;

import static com.harmonycloud.caas.common.constants.CommonConstant.*;
import static com.harmonycloud.caas.common.constants.NameConstant.VG_NAME;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.model.middleware.MiddlewareStorageInfoDto;
import com.harmonycloud.caas.common.model.user.ProjectDto;
import com.harmonycloud.zeus.service.user.ProjectService;
import com.harmonycloud.zeus.util.DateUtil;
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
    @Autowired
    private ProjectService projectService;
    @Autowired
    private NamespaceService namespaceService;

    @Override
    public List<String> getType() {
        return Arrays.stream(StorageClassProvisionerEnum.values()).map(StorageClassProvisionerEnum::getType)
            .collect(Collectors.toList());
    }

    @Override
    public StorageDto get(String clusterId, String name, Boolean detail) {
        StorageClass storageClass = storageClassWrapper.get(clusterId, name);
        return detail ? detail(clusterId,  storageClass) : convert(clusterId, storageClass);
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
            List<StorageDto> storageDtoList = storageClassList.stream().filter(storageClass -> {
                boolean flag = CollectionUtils.isEmpty(storageClass.getMetadata().getAnnotations())
                    || !storageClass.getMetadata().getAnnotations().containsKey(MIDDLEWARE);
                return all == flag;
            }).map(storageClass -> {
                // 初始化业务对象
                    return all ? convert(cluster.getId(), storageClass) : detail(cluster.getId(), storageClass);
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
    public void addOrUpdate(StorageDto storageDto) {
        List<StorageClass> storageClassList = storageClassWrapper.list(storageDto.getClusterId());
        // 校验该存储是否存在
        if (CollectionUtils.isEmpty(storageClassList) || storageClassList.stream()
            .noneMatch(storageClass -> storageClass.getMetadata().getName().equals(storageDto.getName()))) {
            throw new BusinessException(ErrorMessage.STORAGE_CLASS_NOT_FOUND);
        }
        // 校验中文名称
        checkAliasName(storageDto, storageClassList);

        StorageClass storageClass = storageClassList.stream()
            .filter(sc -> sc.getMetadata().getName().equals(storageDto.getName())).collect(Collectors.toList()).get(0);
        // 获取annotationss
        Map<String, String> annotations = storageClass.getMetadata().getAnnotations();
        if (annotations == null) {
            annotations = new HashMap<>();
        }
        annotations.put(MIDDLEWARE, TRUE);
        annotations.put(ALIAS_NAME, storageDto.getAliasName());
        storageClass.getMetadata().setAnnotations(annotations);
        storageClassWrapper.update(storageDto.getClusterId(), storageClass);
    }

    @Override
    public void delete(String clusterId, String storageName) {
        StorageClass storageClass = storageClassWrapper.get(clusterId, storageName);
        if (storageClass == null){
            throw new BusinessException(ErrorMessage.STORAGE_CLASS_NOT_FOUND);
        }
        Map<String, String> annotations = storageClass.getMetadata().getAnnotations();
        if (annotations != null){
            annotations.remove(MIDDLEWARE);
            annotations.remove(ALIAS_NAME);
            storageClassWrapper.update(clusterId, storageClass);
        }
    }

    @Override
    public StorageDto detail(String clusterId, String storageName) {
        StorageClass storageClass = storageClassWrapper.get(clusterId, storageName);
        return detail(clusterId, storageClass);

    }

    public StorageDto detail(String clusterId, StorageClass storageClass){
        StorageDto storageDto = convert(clusterId, storageClass);
        // 查询存储
        List<PersistentVolumeClaim> pvcList = pvcService.list(clusterId, null);
        pvcList = pvcList.stream().filter(pvc -> StringUtils.isNotEmpty(pvc.getStorageClassName())
            && pvc.getStorageClassName().equals(storageDto.getName())).collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        for (PersistentVolumeClaim pvc : pvcList){
            sb.append(pvc.getName()).append("|");
        }
        // 查询申请配额
        String requestQuery = "sum(kube_persistentvolumeclaim_resource_requests_storage_bytes{persistentvolumeclaim=~\""
                + sb.toString() + "\"})/1024/1024/1024";
        double request = prometheusResourceMonitorService.queryAndConvert(clusterId, requestQuery);

        // 查询使用量
        String usedQuery = "sum(kubelet_volume_stats_used_bytes{persistentvolumeclaim=~\""
                + sb.toString() + "\",endpoint!=\"\"}) /1024/1024/1024";
        double used = prometheusResourceMonitorService.queryAndConvert(clusterId, usedQuery);

        MonitorResourceQuota monitorResourceQuota = storageDto.getMonitorResourceQuota();
        if (monitorResourceQuota == null){
            monitorResourceQuota = new MonitorResourceQuota();
        }
        monitorResourceQuota.getStorage().setRequest(request);
        monitorResourceQuota.getStorage().setUsed(used);
        storageDto.setMonitorResourceQuota(monitorResourceQuota);
        return storageDto;
    }

    @Override
    public List<MiddlewareStorageInfoDto> middlewares(String clusterId, String storageName) {
        // 获取所有中间件cr
        List<MiddlewareCR> middlewareCRList = middlewareCRService.listCR(clusterId, null, null);

        // 查询存储
        List<PersistentVolumeClaim> all = pvcService.list(clusterId, null);
        List<PersistentVolumeClaim> pvcList = all.stream().filter(
            pvc -> StringUtils.isNotEmpty(pvc.getStorageClassName()) && pvc.getStorageClassName().equals(storageName))
            .collect(Collectors.toList());

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

        List<MiddlewareStorageInfoDto> mwStorageInfoList = new ArrayList<>();
        for (MiddlewareCR middlewareCr : middlewareCRList){
            MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
            MiddlewareStorageInfoDto mwStoInfo = new MiddlewareStorageInfoDto();
            // 获取pod列表
            String type = middlewareCrTypeService.findTypeByCrType(middlewareCr.getSpec().getType());
            Middleware middleware = podService.list(clusterId, middlewareCr.getMetadata().getNamespace(),
                middlewareCr.getSpec().getName(), type);
            mwStoInfo.setPods(middleware.getPods());
            mwStoInfo.setPodNum(middleware.getPodNum());
            // 转换创建时间
            mwStoInfo.setCreateTime(DateUtils.parseUTCDate(middlewareCr.getMetadata().getCreationTimestamp()));

            List<MiddlewareInfo> pvcNameList = middlewareCr.getStatus().getInclude().get(PERSISTENT_VOLUME_CLAIMS);

            StringBuilder pvcs = new StringBuilder();
            pvcNameList.forEach(pvcName -> pvcs.append(pvcName.getName()).append("|"));

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
                if (totalResult.containsKey(num)){
                    podQuota.getStorage().setTotal(totalResult.get(num));
                    totalStorage = totalStorage + totalResult.get(num);
                }
                if (usingResult.containsKey(num)){
                    podQuota.getStorage().setUsed(usingResult.get(num));
                    usedStorage = usedStorage + usingResult.get(num);
                }
                pod.setMonitorResourceQuota(podQuota);
            }

            MonitorResourceQuota middlewareQuota = new MonitorResourceQuota();
            middlewareQuota.getStorage().setTotal(totalStorage);
            middlewareQuota.getStorage().setUsed(usedStorage);

            mwStoInfo.setMonitorResourceQuota(middlewareQuota);
            JSONObject values = helmChartService.getInstalledValues(middleware, clusterService.findById(clusterId));
            if (values.containsKey("chart-version")){
                mwStoInfo.setImagePath(middleware.getType() + "-" + values.getString("chart-version") + ".svg");
            }
            mwStoInfo.setMiddlewareName(middlewareCr.getSpec().getName());
            mwStoInfo.setStatus(middlewareCr.getStatus().getPhase());
            mwStoInfo.setType(type);
            mwStoInfo.setMiddlewareAliasName(values.getOrDefault("aliasName", "").toString());

            // 获取所在项目
            ProjectDto projectDto = projectService.findProjectByNamespace(middlewareCr.getMetadata().getNamespace());
            mwStoInfo.setProjectId(projectDto.getProjectId());
            mwStoInfo.setProjectAliasName(projectDto.getAliasName());
            // 获取项目名称
            mwStoInfo.setNamespace(middlewareCr.getMetadata().getNamespace());
            mwStoInfo.setNamespaceAliasName(namespaceService.get(clusterId, mwStoInfo.getNamespace()).getAliasName());

            mwStoInfo.setClusterId(clusterId);
            mwStoInfo.setClusterAliasName(cluster.getNickname());

            mwStorageInfoList.add(mwStoInfo);
        }

        return mwStorageInfoList;
    }

    /**
     * 封装业务对象
     */
    public StorageDto convert(String clusterId, StorageClass storageClass){
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        // 初始化业务对象
        StorageDto storageDto = new StorageDto();
        // 获取存储配额
        Map<String, String> annotations = storageClass.getMetadata().getAnnotations();
        // 获取中文名称
        if (!CollectionUtils.isEmpty(annotations) && annotations.containsKey(ALIAS_NAME)) {
            storageDto.setAliasName(annotations.get(ALIAS_NAME));
        }
        // 获取vg_name
        if (storageClass.getParameters() != null && storageClass.getParameters().containsKey(VG_NAME)){
            storageDto.setVgName(storageClass.getParameters().get(VG_NAME));
        }

        storageDto.setClusterId(clusterId);
        storageDto.setName(storageClass.getMetadata().getName());
        storageDto.setProvisioner(storageClass.getProvisioner());
        storageDto.setClusterAliasName(cluster.getNickname());
        storageDto.setCreateTime(DateUtil.StringToDate(storageClass.getMetadata().getCreationTimestamp(), DateType.YYYY_MM_DD_T_HH_MM_SS_Z));

        // 获取类型
        String type = StorageClassProvisionerEnum.findByProvisioner(storageClass.getProvisioner()).getType();
        storageDto.setVolumeType(type == null ? "unknown" : type);

        return storageDto;
    }

    /**
     * 校验中文名称是否已存在
     */
    public void checkAliasName(StorageDto storageDto, List<StorageClass> storageClassList) {
        for (StorageClass storageClass : storageClassList) {
            Map<String, String> annotations = storageClass.getMetadata().getAnnotations();
            if (CollectionUtils.isEmpty(annotations)) {
                return;
            }
            if (annotations.containsKey(ALIAS_NAME) && annotations.get(ALIAS_NAME).equals(storageDto.getAliasName())
                && !storageClass.getMetadata().getName().equals(storageDto.getName())) {
                throw new BusinessException(ErrorMessage.STORAGE_CLASS_NAME_EXIST);
            }
        }
    }

}
