package com.middleware.zeus.service.k8s.impl;

import static com.middleware.zeus.common.constants.CommonConstant.*;
import static com.middleware.zeus.common.constants.NameConstant.*;
import static com.middleware.zeus.common.constants.NameConstant.TRUE;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.middleware.zeus.common.enums.DateType;
import com.middleware.zeus.common.enums.DictEnum;
import com.middleware.zeus.common.model.QuotaBase;
import com.middleware.zeus.common.model.middleware.*;
import com.middleware.zeus.common.model.user.ProjectNamespaceDo;
import com.middleware.zeus.service.middleware.MiddlewarePvcService;
import com.middleware.zeus.util.uuid.UUIDUtils;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.util.DateUtil;
import com.middleware.zeus.service.k8s.*;
import io.fabric8.kubernetes.api.model.TopologySelectorLabelRequirement;
import io.fabric8.kubernetes.api.model.TopologySelectorTerm;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.enums.middleware.StorageClassProvisionerEnum;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.MonitorResourceQuota;
import com.middleware.zeus.common.model.PersistentVolumeClaim;
import com.middleware.zeus.common.model.StorageDto;
import com.middleware.zeus.util.date.DateUtils;
import com.middleware.zeus.integration.cluster.StorageClassWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareInfo;
import com.middleware.zeus.service.middleware.MiddlewareCrTypeService;
import com.middleware.zeus.service.prometheus.PrometheusResourceMonitorService;
import com.middleware.zeus.service.registry.HelmChartService;

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
    @Autowired
    private MiddlewarePvcService middlewarePvcService;

    @Override
    public List<String> getType() {
        return Arrays.stream(StorageClassProvisionerEnum.values()).map(StorageClassProvisionerEnum::getType)
                .collect(Collectors.toList());
    }

    @Override
    public StorageDto get(String clusterId, String name) {
        StorageClass storageClass = storageClassWrapper.get(clusterId, name);
        return convert(clusterId, storageClass);
    }

    @Override
    public List<StorageDto> list(String clusterId, Boolean all) {
        List<StorageClass> storageClassList = storageClassWrapper.list(clusterId);
        return storageClassList.stream()
                .filter(storageClass -> all || (!CollectionUtils.isEmpty(storageClass.getMetadata().getAnnotations())
                        && storageClass.getMetadata().getAnnotations().containsKey(MIDDLEWARE)))
                .map(storageClass -> convert(clusterId, storageClass)).collect(Collectors.toList());
    }

    @Override
    public StorageDto getById(String clusterId, String storageId) {
        List<StorageDto> storageDtoList = this.list(clusterId, null, null, false);
        if (CollectionUtils.isEmpty(storageDtoList)) {
            return null;
        }
        storageDtoList = storageDtoList.stream().filter(storageDto -> storageDto.getStorageId().equals(storageId))
            .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(storageDtoList)) {
            return storageDtoList.get(0);
        }
        return null;
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
            Set<String> aliasNameSet = new HashSet<>();
            List<StorageDto> storageDtoList = storageClassList.stream().filter(storageClass -> {
                if (storageClass.getMetadata() == null || storageClass.getMetadata().getAnnotations() == null) {
                    return all;
                }
                boolean flag = CollectionUtils.isEmpty(storageClass.getMetadata().getAnnotations())
                        || !storageClass.getMetadata().getAnnotations().containsKey(MIDDLEWARE);
                // 双活只保留一个避免重复
                if (storageClass.getMetadata().getAnnotations().get(ALIAS_NAME) == null) {
                    return all == flag;
                }
                return all == flag && aliasNameSet.add(storageClass.getMetadata().getAnnotations().get(ALIAS_NAME));
            }).map(storageClass -> {
                // 初始化业务对象
                return convert(cluster.getId(), storageClass);
            }).filter(storageDto -> {
                if (StringUtils.isNotEmpty(key)) {
                    return storageDto.getAliasName().contains(key) || storageDto.getStorageClassList().stream().anyMatch(sc -> sc.getName().contains(key));
                }
                return true;
            }).filter(storageDto -> {
                if (StringUtils.isNotEmpty(type)) {
                    return storageDto.getStorageClassList().stream().anyMatch(sc -> type.equals(sc.getVolumeType()));
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
        Map<String, StorageClass> scMap = storageClassList.stream().collect(Collectors.toMap(s -> s.getMetadata().getName(), sc -> sc));

        // 校验中文名称
        checkAliasName(storageDto, storageClassList);

        List<StorageClass> scList = storageDto.getStorageClassList().stream().map(sc -> {
            StorageClass storageClass = scMap.get(sc.getName());
            // 校验该存储是否存在
            if (storageClass == null) {
                throw new BusinessException(DictEnum.STORAGE_CLASS,sc.getName(),ErrorMessage.NOT_EXIST);
            }
            return storageClass;
        }).collect(Collectors.toList());

        Date integrateTime = new Date();
        String storageId = UUIDUtils.get16UUID();
        for (StorageClass sc : scList) {
            //获取annotations
            Map<String, String> annotations = sc.getMetadata().getAnnotations();
            if (annotations == null) {
                annotations = new HashMap<>();
            }
            annotations.put(MIDDLEWARE, TRUE);
            annotations.put(ALIAS_NAME, storageDto.getAliasName());
            annotations.put(TOTAL_STORAGE, storageDto.getTotalStorage().toString());
            // 设置接入时间
            if (!annotations.containsKey(INTEGRATE_TIME)) {
                annotations.put(INTEGRATE_TIME,
                        DateUtils.DateToString(integrateTime, DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue()));
            }
            // 设置存储服务id
            if (!annotations.containsKey(STORAGE_ID)){
                annotations.put(STORAGE_ID, storageId);
            }
            // 双活配置，保留annotations中的该key，values没有意义，可以考虑修改为true
            if (storageDto.getIsActiveActive()) {
                String active = scList.stream().filter(storageClass -> !storageClass.getMetadata().getName().equals(sc.getMetadata().getName()))
                        .collect(Collectors.toList()).get(0).getMetadata().getName();
                annotations.put(ACTIVE_ACTIVE, active);
            }
            sc.getMetadata().setAnnotations(annotations);
            storageClassWrapper.update(storageDto.getClusterId(), sc);
        }

    }

    @Override
    public void delete(String clusterId, String storageId) {
        List<StorageClass> storageClassList = storageClassWrapper.list(clusterId);
        List<StorageClass> scList = storageClassList.stream()
            .filter(storageClass -> !CollectionUtils.isEmpty(storageClass.getMetadata().getAnnotations())
                && storageClass.getMetadata().getAnnotations().containsKey(STORAGE_ID)
                && storageId.equals(storageClass.getMetadata().getAnnotations().get(STORAGE_ID)))
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(scList)) {
            throw new BusinessException(ErrorMessage.STORAGE_CLASS_NOT_FOUND);
        }
        // 查询存储
        List<PersistentVolumeClaim> pvcList = pvcService.list(clusterId, null);
        pvcList = pvcList.stream().filter(pvc -> StringUtils.isNotEmpty(pvc.getStorageClassName())
                && scList.stream().anyMatch(sc -> pvc.getStorageClassName().equals(sc.getMetadata().getName()))).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(pvcList)) {
            throw new BusinessException(ErrorMessage.STORAGE_CLASS_IS_BEING_USED);
        }
        for (StorageClass sc : scList) {
            Map<String, String> annotations = sc.getMetadata().getAnnotations();
            annotations.remove(MIDDLEWARE);
            annotations.remove(ALIAS_NAME);
            annotations.remove(INTEGRATE_TIME);
            // 已取消使用，保留移除的逻辑
            annotations.remove(ACTIVE_ACTIVE);
            annotations.remove(TOTAL_STORAGE);
            annotations.remove(STORAGE_ID);
            storageClassWrapper.update(clusterId, sc);
        }
    }


    @Override
    public Map<String, Map<String, QuotaBase>> monitorStorageQuota(String clusterId) {
        // 获取要查询的集群
        List<MiddlewareClusterDTO> clusterList = new ArrayList<>();
        if (clusterId.equals(ASTERISK)) {
            clusterList = clusterService.listClusters();
        } else {
            clusterList.add(clusterService.findById(clusterId));
        }
        Map<String, Map<String, QuotaBase>> result = new HashMap<>();
        clusterList.forEach(cluster -> {
            List<PersistentVolumeClaim> allPvc = pvcService.list(cluster.getId(), null);
            Map<String, StringBuilder> aliasPvcMap = new HashMap<>();
            List<StorageClass> scList = storageClassWrapper.list(cluster.getId());
            Map<String, StorageClass> scMap = scList.stream().collect(Collectors.toMap(sc -> sc.getMetadata().getName(), sc -> sc));
            // 对所有pvc按aliasName分类
            for (PersistentVolumeClaim pvc : allPvc) {
                StorageClass storageClass = scMap.get(pvc.getStorageClassName());
                if (storageClass == null) {
                    continue;
                }
                Map<String, String> annotations = storageClass.getMetadata().getAnnotations();
                if (CollectionUtils.isEmpty(annotations) || !annotations.containsKey(MIDDLEWARE)) {
                    continue;
                }
                String aliasName = annotations.get(ALIAS_NAME);
                if (aliasPvcMap.containsKey(aliasName)) {
                    aliasPvcMap.get(aliasName).append("|").append(pvc.getVolumeName());
                } else {
                    aliasPvcMap.put(aliasName, new StringBuilder().append(pvc.getVolumeName()));
                }
            }
            Map<String, QuotaBase> aliasQuotaMap = new HashMap<>();
            // 查询Quota
            for (String aliasName : aliasPvcMap.keySet()) {
                StringBuilder sb = aliasPvcMap.get(aliasName);
                // 查询申请配额
                String requestQuery = "sum(total_size_kb{pv=~\"" + sb.toString() + "\"})/1024/1024";
                double request = prometheusResourceMonitorService.queryAndConvert(cluster.getId(), requestQuery);

                // 查询使用量
                String usedQuery = "sum(used_size_kb{pv=~\"" + sb.toString() + "\",endpoint!=\"\"}) /1024/1024";
                double used = prometheusResourceMonitorService.queryAndConvert(cluster.getId(), usedQuery);

                // 封装数据
                QuotaBase quotaBase = new QuotaBase();
                quotaBase.setRequest(request);
                quotaBase.setUsed(used);
                aliasQuotaMap.put(aliasName, quotaBase);
            }
            result.put(cluster.getId(), aliasQuotaMap);
        });
        return result;
    }

    @Override
    public Map<String, String> listStorageMap(String clusterId, Boolean all) {
        List<StorageDto> storageDtoList = list(clusterId, all);
        Map<String, String> scAliasNameMap = new HashMap<>();
        storageDtoList.forEach(storageDto -> {
            String aliasName = StringUtils.isNotBlank(storageDto.getAliasName()) ? storageDto.getAliasName() : storageDto.getName();
            scAliasNameMap.put(storageDto.getName(), aliasName);
        });
        return scAliasNameMap;
    }


    @Override
    public List<MiddlewareStorageInfoDto> middlewares(String clusterId, String storageName) {
        // 获取所有中间件cr
        List<MiddlewareCR> middlewareCRList = middlewareCRService.listCR(clusterId, null, null);

        // 过滤受保护的分区
        middlewareCRList = middlewareCRList.stream().filter(middlewareCR ->
                !namespaceService.isNamespacceProtected(middlewareCR.getMetadata().getNamespace())).collect(Collectors.toList());

        List<String> storageNameList = new ArrayList<>();
        StorageClass storageClass = storageClassWrapper.get(clusterId, storageName);
        storageNameList.add(storageClass.getMetadata().getName());
        Map<String, String> annotations = storageClass.getMetadata().getAnnotations();
        if (!CollectionUtils.isEmpty(annotations) && annotations.get(ACTIVE_ACTIVE) != null) {
            StorageClass activeStorageClass = storageClassWrapper.get(clusterId, annotations.get(ACTIVE_ACTIVE));
            storageNameList.add(activeStorageClass.getMetadata().getName());
        }
        // 查询存储
        List<PersistentVolumeClaim> all = pvcService.list(clusterId, null);
        List<PersistentVolumeClaim> pvcList = all.stream().filter(
                pvc -> StringUtils.isNotEmpty(pvc.getStorageClassName()) && storageNameList.contains(pvc.getStorageClassName()))
                .collect(Collectors.toList());

        // 过滤获取到使用了该存储的中间件
        middlewareCRList = middlewareCRList.stream().filter(middlewareCr -> {
            Map<String, List<MiddlewareInfo>> include = middlewareCr.getStatus().getInclude();
            if (CollectionUtils.isEmpty(include) || !include.containsKey(PERSISTENT_VOLUME_CLAIMS)) {
                return false;
            }
            List<MiddlewareInfo> middlewarePvcList = include.get(PERSISTENT_VOLUME_CLAIMS);
            return middlewarePvcList.stream().anyMatch(middlewarePvc ->
                    pvcList.stream().anyMatch(pvc -> middlewarePvc.getName().equals(pvc.getName())));
        }).collect(Collectors.toList());
        
        // 获取所有分区对应项目 用于设置中间件所在项目
        List<ProjectNamespaceDo> projectNsList = projectService.listNamespace(clusterId);
        Map<String, ProjectNamespaceDo> projectNsMap =
            projectNsList.stream().collect(Collectors.toMap(ProjectNamespaceDo::getNamespace, Function.identity()));
        

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

            List<PersistentVolumeClaim> mwPvcList = middlewarePvcService.listMiddlewarePvc(clusterId, middlewareCr.getMetadata().getNamespace(), middlewareCr.getSpec().getName(), type);
            StringBuilder pvcs = new StringBuilder();
            for (PersistentVolumeClaim pvc : mwPvcList) {
                pvcs.append(pvc.getVolumeName()).append("|");
            }

            // 查询storage request
            String totalStorageQuery = "sum(total_size_kb{pv=~\"" + pvcs.toString() + "\"}) /1024/1024";
            double totalResult = prometheusResourceMonitorService.queryAndConvert(clusterId, totalStorageQuery);

            // 查询storage using
            String usedStorageQuery = "sum(used_size_kb{pv=~\"" + pvcs.toString() + "\"}) /1024/1024";
            double usingResult = prometheusResourceMonitorService.queryAndConvert(clusterId, usedStorageQuery);

            // 封装数据

            MonitorResourceQuota middlewareQuota = new MonitorResourceQuota();
            middlewareQuota.getStorage().setTotal(totalResult);
            middlewareQuota.getStorage().setUsed(usingResult);

            mwStoInfo.setMonitorResourceQuota(middlewareQuota);
            JSONObject values = helmChartService.getInstalledValues(middleware, clusterService.findById(clusterId));
            if (values == null) {
                continue;
            }
            if (values.containsKey("chart-version")){
                mwStoInfo.setImagePath(middleware.getType() + "-" + values.getString("chart-version") + ".svg");
            }
            mwStoInfo.setMiddlewareName(middlewareCr.getSpec().getName());
            mwStoInfo.setStatus(middlewareCr.getStatus().getPhase());
            mwStoInfo.setType(type);
            mwStoInfo.setMiddlewareAliasName(values.getOrDefault("aliasName", "").toString());

            // 设置中间件所在项目
            if (projectNsMap.containsKey(middlewareCr.getMetadata().getNamespace())){
                ProjectNamespaceDo projectNsDo = projectNsMap.get(middlewareCr.getMetadata().getNamespace());
                mwStoInfo.setProjectId(projectNsDo.getProjectId());
                mwStoInfo.setProjectAliasName(projectNsDo.getProjectName());
            }

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
    public StorageDto convert(String clusterId, StorageClass storageClass) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        // 初始化业务对象
        StorageDto storageDto = new StorageDto();
        // 获取存储配额
        Map<String, String> annotations = storageClass.getMetadata().getAnnotations();
        if (CollectionUtils.isEmpty(annotations)) {
            annotations = new HashMap<>();
        }
        // 获取中文名称
        if (annotations.containsKey(ALIAS_NAME)) {
            storageDto.setAliasName(annotations.get(ALIAS_NAME));
        }
        // 获取接入时间
        if (annotations.containsKey(INTEGRATE_TIME)) {
            storageDto.setCreateTime(
                DateUtil.StringToDate(annotations.get(INTEGRATE_TIME), DateType.YYYY_MM_DD_T_HH_MM_SS_Z));
        }
        // 获取配额总额
        if (annotations.containsKey(TOTAL_STORAGE)) {
            storageDto.setTotalStorage(Double.parseDouble(annotations.get(TOTAL_STORAGE)));
        }
        // 获取存储id
        if (annotations.containsKey(STORAGE_ID)) {
            storageDto.setStorageId(annotations.get(STORAGE_ID));
        }
        storageDto.setClusterId(clusterId);
        storageDto.setClusterAliasName(cluster.getNickname());
        storageDto.setIsActiveActive(false);
        storageDto.getStorageClassList().add(convertSc(storageClass));

        // 双活配置
        if (annotations.containsKey(ACTIVE_ACTIVE)) {
            storageDto.setIsActiveActive(true);
            // 获取双活添加的其他sc
            List<StorageClass> storageClassList = storageClassWrapper.list(clusterId);
            if (CollectionUtils.isEmpty(storageClassList)) {
                return storageDto;
            }
            // 获取环境中其他的storageId与当前storageClass的id相同的sc
            storageClassList = storageClassList.stream()
                .filter(sc -> sc.getMetadata().getAnnotations() != null
                    && sc.getMetadata().getAnnotations().get("storageId") != null && storageDto.getStorageId() != null
                    && sc.getMetadata().getAnnotations().get("storageId").equals(storageDto.getStorageId()))
                .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(storageClassList)) {
                return storageDto;
            }
            // 再从中过滤掉当前已封装的sc
            storageClassList = storageClassList.stream()
                .filter(sc -> !sc.getMetadata().getName().equals(storageClass.getMetadata().getName()))
                .collect(Collectors.toList());
            for (StorageClass sc : storageClassList) {
                storageDto.getStorageClassList().add(convertSc(sc));
            }
        }
        return storageDto;
    }

    public StorageClassInfo convertSc(StorageClass storageClass) {
        StorageClassInfo sc = new StorageClassInfo();
        // 获取vg_name
        if (storageClass.getParameters() != null && storageClass.getParameters().containsKey(VG_NAME)){
            sc.setVgName(storageClass.getParameters().get(VG_NAME));
        }
        sc.setName(storageClass.getMetadata().getName());
        sc.setProvisioner(storageClass.getProvisioner());
        // 获取类型
        StorageClassProvisionerEnum provisionerEnum = StorageClassProvisionerEnum.findByProvisioner(storageClass.getProvisioner());
        String type = null;
        if (provisionerEnum != null) {
            type = provisionerEnum.getType();
        }
        sc.setVolumeType(type == null ? storageClass.getProvisioner() : type);
        // 获取双活分区
        loop :for (TopologySelectorTerm tst: storageClass.getAllowedTopologies()) {
            List<TopologySelectorLabelRequirement> matchLabelExpressions = tst.getMatchLabelExpressions();
            for (TopologySelectorLabelRequirement tsr:matchLabelExpressions) {
                if (STORAGE_ZONE.equals(tsr.getKey())) {
                    sc.setActiveZone(tsr.getValues().get(0));
                    break loop;
                }
            }
        }
        // 设置存储id
        if(storageClass.getMetadata().getAnnotations() != null && storageClass.getMetadata().getAnnotations().containsKey(STORAGE_ID)){
            sc.setStorageId(storageClass.getMetadata().getAnnotations().get(STORAGE_ID));
        }
        sc.setParameters(storageClass.getParameters());
        return sc;
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
                    && storageDto.getStorageClassList().stream().noneMatch(sc -> storageClass.getMetadata().getName().equals(sc.getName()))) {
                throw new BusinessException(ErrorMessage.STORAGE_CLASS_NAME_EXIST);
            }
        }
    }

    @Override
    public String getAliasName(String clusterId, String storageName) {

        StorageClass storageClass = storageClassWrapper.get(clusterId, storageName);
        if (storageClass == null) {
            throw new BusinessException(DictEnum.STORAGE_CLASS, storageName, ErrorMessage.NOT_FOUND);
        }
        Map<String, String> annotations = storageClass.getMetadata().getAnnotations();
        if (CollectionUtils.isEmpty(annotations) || !annotations.containsKey(ALIAS_NAME)) {
            return null;
        }
        return annotations.get(ALIAS_NAME);
    }

    @Override
    public Map<String, String> checkHitachiAndGetParams(String clusterId, String storageName) {
        if (StringUtils.isEmpty(storageName)){
            return new HashMap<>();
        }
        if (storageName.contains(",")){
            storageName = storageName.split(",")[0];
        }else if (storageName.contains("/")){
            storageName = storageName.split("/")[0];
        }
        Map<String, String> params = new HashMap<>();
        StorageClass storageClass = storageClassWrapper.get(clusterId, storageName);
        if (storageClass.getProvisioner().equals(StorageClassProvisionerEnum.HITACHI.getProvisioner())
                && !CollectionUtils.isEmpty(storageClass.getParameters())){
            params = storageClass.getParameters();
        }
        return params;
    }

    @Override
    public List<StorageClassInfo> listStorageClassInfo(String clusterId, Boolean all) {
        // todo 根据all 是否为false  过滤掉未被平台纳管的storageClass
        return storageClassWrapper.list(clusterId).stream().map(this::convertSc).collect(Collectors.toList());
    }


}
