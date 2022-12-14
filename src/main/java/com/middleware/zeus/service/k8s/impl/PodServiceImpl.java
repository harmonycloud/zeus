package com.middleware.zeus.service.k8s.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.constants.DateStyle;
import com.middleware.caas.common.enums.DictEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.caas.common.enums.middleware.ResourceUnitEnum;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.ContainerWithStatus;
import com.middleware.caas.common.model.Node;
import com.middleware.caas.common.model.StorageClassDTO;
import com.middleware.caas.common.model.StorageDto;
import com.middleware.caas.common.model.middleware.*;
import com.middleware.tool.numeric.ResourceCalculationUtil;
import com.middleware.zeus.bean.BeanActiveArea;
import com.middleware.zeus.integration.cluster.PodWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.integration.cluster.bean.MiddlewareInfo;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.service.middleware.impl.MiddlewareBackupServiceImpl;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.util.DateUtil;
import com.middleware.zeus.util.MathUtil;
import com.middleware.zeus.util.RedisUtil;
import com.middleware.zeus.service.k8s.*;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.middleware.caas.common.constants.NameConstant.CPU;
import static com.middleware.caas.common.constants.NameConstant.MEMORY;
import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.PERSISTENT_VOLUME_CLAIMS;
import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.PODS;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Service
public class PodServiceImpl implements PodService {

    @Value("${active-active.label.key:topology.kubernetes.io/zone}")
    private String zoneKey;

    @Autowired
    private PodWrapper podWrapper;
    @Autowired
    private MiddlewareCRService middlewareCRService;
    @Autowired
    private StorageClassService storageClassService;
    @Autowired
    private MiddlewareBackupServiceImpl middlewareBackupService;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private ActiveAreaService activeAreaService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private StorageService storageService;

    @Override
    public Middleware list(String clusterId, String namespace, String middlewareName, String type) {
        MiddlewareCR mw = middlewareCRService.getCR(clusterId, namespace, type, middlewareName);
        Middleware middleware = listPodsWithMiddleware(mw, clusterId, namespace, middlewareName, type);
        middleware.setHasConfigBackup(middlewareBackupService.checkIfAlreadyBackup(clusterId, middleware.getNamespace(), middleware.getType(), middleware.getName()));
        return middleware;
    }

    @Override
    public List<PodInfo> list(String clusterId, String namespace) {
        List<Pod> list = podWrapper.list(clusterId, namespace);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        return list.stream().map(this::convertPodInfo).collect(Collectors.toList());
    }

    @Override
    public List<PodInfo> list(String clusterId, String namespace, String key) {
        List<Pod> list = podWrapper.list(clusterId, namespace).stream()
            .filter(pod -> pod.getMetadata().getName().contains(key)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        return list.stream().map(this::convertPodInfo).collect(Collectors.toList());
    }

    @Override
    public List<PodInfo> list(String clusterId, String namespace, Map<String, String> labels) {
        List<Pod> list = podWrapper.list(clusterId, namespace, labels);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        List<PodInfo> podInfoList = list.stream().map(this::convertPodInfo).collect(Collectors.toList());
        podInfoList.forEach(podInfo -> {
            podInfo.setCreateTime(DateUtil.utc2Local(podInfo.getCreateTime(), "yyyy-MM-dd HH:mm:ss", DateStyle.YYYY_MM_DD_HH_MM_SS));
        });
        return podInfoList;
    }

    @Override
    public List<PodInfo> listByFields(String clusterId, String namespace, Map<String, String> fields) {
        List<Pod> list = podWrapper.listByFields(clusterId, namespace, fields);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        return list.stream().map(this::convertPodInfo).collect(Collectors.toList());
    }

    /**
     * 将podlist进行分组
     * @param podInfoList
     * @return
     */
    private PodInfoGroup convertPodListToGroup(List<PodInfo> podInfoList) {
        PodInfoGroup podInfoGroup = new PodInfoGroup();
        Map<String, List<PodInfo>> podMap = new HashMap<>();
        // 根据pod role进行分组，k、v分别为节点角色、相同角色节点列表
        podInfoList.forEach(podInfo -> {
            String role = StringUtils.isNotBlank(podInfo.getGroup()) ? podInfo.getGroup() : podInfo.getRole();
            List<PodInfo> infoList;
            if (role == null) {
                infoList = podMap.get("default");
                if (CollectionUtils.isEmpty(infoList)) {
                    infoList = new ArrayList<>();
                }
                podMap.put("default", infoList);
            } else {
                infoList = podMap.get(role);
                if (CollectionUtils.isEmpty(infoList)) {
                    infoList = new ArrayList<>();
                }
                podMap.put(role, infoList);
            }
            infoList.add(podInfo);
        });

        // 转为列表
        if (podMap.keySet().size() > 1) {
            List<PodInfoGroup> list = new ArrayList<>();
            podMap.forEach((k, v) -> {
                PodInfoGroup singlePodInfoGroup = new PodInfoGroup();
                singlePodInfoGroup.setRole(k);
                singlePodInfoGroup.setPods(v);
                singlePodInfoGroup.setHasChildGroup(false);
                list.add(singlePodInfoGroup);
            });
            podInfoGroup.setHasChildGroup(true);
            podInfoGroup.setListChildGroup(list);
        } else {
            podMap.forEach((k, v) -> {
                List<PodInfoGroup> list = new ArrayList<>();
                PodInfoGroup tempGroup = new PodInfoGroup();
                tempGroup.setRole(k);
                tempGroup.setPods(v);
                tempGroup.setHasChildGroup(false);
                list.add(tempGroup);
                podInfoGroup.setListChildGroup(list);
                podInfoGroup.setHasChildGroup(true);
            });
        }
        return podInfoGroup;
    }

    /**
     * 获取pod真实状态
     * @param pod
     * @return
     */
    private String getPodRealState(Pod pod) {
        // 1. Terminating
        if (StringUtils.isNotBlank(pod.getMetadata().getDeletionTimestamp())) {
            return "Terminating";
        }

        // 2. Complete
        if ("Succeeded".equals(pod.getStatus().getPhase())) {
            return "Completed";
        }

        // 3. container not ready
        List<ContainerStatus> containerStatuses = pod.getStatus().getContainerStatuses();
        for (ContainerStatus containerStatus : containerStatuses) {
            if (!containerStatus.getReady()) {
                return "NotReady";
            }
        }
        return pod.getStatus().getPhase();
    }

    private PodInfo convertPodInfo(Pod pod) {
        PodInfo pi = new PodInfo()
                .setPodName(pod.getMetadata().getName())
                .setPodIp(pod.getStatus().getPodIP())
                .setNodeName(pod.getSpec().getNodeName())
                .setCreateTime(pod.getMetadata().getCreationTimestamp())
                .setRestartCount(0)
                .setNamespace(pod.getMetadata().getNamespace())
                .setHostIp(pod.getStatus().getHostIP());
        // set pod status
        pi.setStatus(getPodRealState(pod));

        // restart count and time
        for (ContainerStatus containerStatus : pod.getStatus().getContainerStatuses()) {
            if (containerStatus.getRestartCount() > pi.getRestartCount()) {
                pi.setRestartCount(containerStatus.getRestartCount());
                if (containerStatus.getLastState() != null && containerStatus.getLastState().getTerminated() != null) {
                    pi.setLastRestartTime(containerStatus.getLastState().getTerminated().getFinishedAt());
                }
            }
        }
        // initContainer
        if (!CollectionUtils.isEmpty(pod.getSpec().getInitContainers())) {
            pi.setInitContainers(pod.getSpec().getInitContainers().stream().map(container -> {
                ContainerWithStatus cs = new ContainerWithStatus();
                cs.setName(container.getName());
                return cs;
            }).collect(Collectors.toList()));
        }
        // container
        List<Container> containers = pod.getSpec().getContainers();
        pi.setContainers(containers.stream().map(container -> {
            ContainerWithStatus cs = new ContainerWithStatus();
            cs.setName(container.getName());
            return cs;
        }).collect(Collectors.toList()));

        // resource
        MiddlewareQuota resource = new MiddlewareQuota();
        double cpu = 0.0;
        double memory = 0.0;
        double limitCpu = 0.0;
        double limitMemory = 0.0;
        for (Container container : containers) {
            if (container.getName().contains("init")) {
                continue;
            }
            ResourceRequirements resources = container.getResources();
            if (!CollectionUtils.isEmpty(resources.getRequests())) {
                if (resources.getRequests().containsKey(CPU)) {
                    cpu = cpu + ResourceCalculationUtil.getResourceValue(resources.getRequests().get(CPU).toString(),
                        CPU, "");
                    resource.setCpu(String.valueOf(ResourceCalculationUtil
                        .getResourceValue(resources.getRequests().get(CPU).toString(), CPU, "")));
                }
                if (resources.getRequests().containsKey(MEMORY)) {
                    memory = memory + ResourceCalculationUtil.getResourceValue(
                        resources.getRequests().get(MEMORY).toString(), MEMORY, ResourceUnitEnum.GI.getUnit());
                    resource.setMemory(String.valueOf(ResourceCalculationUtil.getResourceValue(
                        resources.getRequests().get(MEMORY).toString(), MEMORY, ResourceUnitEnum.GI.getUnit())));
                }
            }
            if (!CollectionUtils.isEmpty(resources.getLimits())) {
                if (resources.getLimits().containsKey(CPU)) {
                    limitCpu = limitCpu
                        + ResourceCalculationUtil.getResourceValue(resources.getLimits().get(CPU).toString(), CPU, "");
                    resource.setLimitCpu(String.valueOf(
                        ResourceCalculationUtil.getResourceValue(resources.getLimits().get(CPU).toString(), CPU, "")));
                }
                if (resources.getLimits().containsKey(MEMORY)) {
                    limitMemory = limitMemory + ResourceCalculationUtil.getResourceValue(
                        resources.getLimits().get(MEMORY).toString(), MEMORY, ResourceUnitEnum.GI.getUnit());
                    resource.setLimitMemory(String.valueOf(ResourceCalculationUtil.getResourceValue(
                        resources.getLimits().get(MEMORY).toString(), MEMORY, ResourceUnitEnum.GI.getUnit())));
                }
            }
        }
        resource.setCpu(
            String.valueOf(ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(cpu), 2, RoundingMode.CEILING)));
        resource.setMemory(
            String.valueOf(ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(memory), 2, RoundingMode.CEILING)));
        resource.setLimitCpu(
            String.valueOf(ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(limitCpu), 2, RoundingMode.CEILING)));
        resource.setLimitMemory(String
            .valueOf(ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(limitMemory), 2, RoundingMode.CEILING)));
        return pi.setResources(resource);
    }

    @Override
    public void restart(String clusterId, String namespace, String middlewareName, String type, String podName) {
        checkExist(clusterId, namespace, middlewareName, type, podName);
        podWrapper.delete(clusterId, namespace, podName);
    }

    @Override
    public void restart(String clusterId, String namespace, String podName) {
        checkExist(clusterId, namespace, podName);
        podWrapper.delete(clusterId, namespace, podName);
    }

    @Override
    public String yaml(String clusterId, String namespace, String middlewareName, String type, String podName) {
        checkExist(clusterId, namespace, middlewareName, type, podName);
        Yaml yaml = new Yaml();
        return yaml.dumpAsMap(podWrapper.get(clusterId, namespace, podName));
    }

    public void checkExist(String clusterId, String namespace, String middlewareName, String type, String podName){
        MiddlewareCR mw = middlewareCRService.getCR(clusterId, namespace, type, middlewareName);
        if (mw == null) {
            throw new BusinessException(DictEnum.MIDDLEWARE, middlewareName, ErrorMessage.NOT_EXIST);
        }
        if (CollectionUtils.isEmpty(mw.getStatus().getInclude())) {
            throw new BusinessException(ErrorMessage.FIND_POD_IN_MIDDLEWARE_FAIL);
        }
        List<MiddlewareInfo> pods = mw.getStatus().getInclude().get(PODS);
        if (CollectionUtils.isEmpty(pods) || pods.stream().noneMatch(po -> podName.equals(po.getName()))) {
            throw new BusinessException(ErrorMessage.FIND_POD_IN_MIDDLEWARE_FAIL);
        }
    }

    @Override
    public String yaml(String clusterId, String namespace, String podName) {
        Yaml yaml = new Yaml();
        return yaml.dumpAsMap(podWrapper.get(clusterId, namespace, podName));
    }

    public void checkExist(String clusterId, String namespace, String podName) {
        Pod pod = podWrapper.get(clusterId, namespace, podName);
        if (pod == null) {
            throw new BusinessException(ErrorMessage.POD_NOT_EXIST);
        }
    }

    /**
     * 为pod设置绑定的pvc
     * @param podInfo pod信息
     * @param pvcInfos 服务所有pvc
     */
    private void setPodPvc(PodInfo podInfo, List<MiddlewareInfo> pvcInfos) {
        List<String> pvcs = new ArrayList<>();
        pvcInfos.forEach(pvc -> {
            if (pvc.getName().contains(podInfo.getPodName())) {
                pvcs.add(pvc.getName());
            }
        });
        podInfo.setPvcs(pvcs);
    }

    /**
     * 设置Pod备份状态
     *
     * @param clusterId      集群id
     * @param namespace      分区
     * @param type           类型
     * @param middlewareName 中间件名称
     * @param podInfo        pod信息
     */
    private void setPodBackupStatus(String clusterId, String namespace, String type, String middlewareName, PodInfo podInfo) {
        podInfo.setHasConfigBackup(middlewareBackupService.checkIfAlreadyBackup(clusterId, namespace, type, middlewareName, podInfo.getPodName()));
    }

    /**
     * 设置pod所在的可用区
     * @param clusterId 集群id
     * @param podInfoList pod集合
     */
    private void setPodArea(String clusterId, List<PodInfo> podInfoList) {
        List<Node> nodeList = nodeService.list(clusterId);
        Map<String, Node> nodeMap = nodeList.stream().collect(Collectors.toMap(Node::getName, Node -> Node));
        podInfoList.forEach(podInfo -> {
            Node node = nodeMap.get(podInfo.getNodeName());
            if (node != null && node.getLabels() != null && node.getLabels().containsKey(zoneKey)) {
                String areaName = node.getLabels().get(zoneKey);
                BeanActiveArea beanActiveArea = activeAreaService.get(clusterId, areaName);
                if (beanActiveArea == null) {
                    podInfo.setNodeZone(areaName);
                } else {
                    podInfo.setNodeZone(beanActiveArea.getAliasName());
                }
            } else {
                podInfo.setNodeZone("");
            }
        });
    }

    @Override
    public Middleware listPodsWithMiddleware(MiddlewareCR mw, String clusterId, String namespace, String middlewareName, String type){
        if (mw == null) {
            throw new BusinessException(DictEnum.MIDDLEWARE, middlewareName, ErrorMessage.NOT_EXIST);
        }
        Middleware middleware = middlewareCRService.simpleConvert(mw);
        if (CollectionUtils.isEmpty(mw.getStatus().getInclude()) || CollectionUtils.isEmpty(mw.getStatus().getInclude().get(PODS))) {
            middleware.setPods(new ArrayList<>(0));
            return middleware;
        }
        List<MiddlewareInfo> pvcInfos = mw.getStatus().getInclude().get(PERSISTENT_VOLUME_CLAIMS);
        Map<String, StorageClassDTO> scMap = storageClassService.convertStorageClass(pvcInfos, clusterId, namespace);
        List<StorageDto> storageDtoList = storageService.list(clusterId, true);
        Map<String, String> scAliasNameMap = new HashMap<>();
        storageDtoList.forEach(storageDto -> {
            String aliasName = StringUtils.isNotBlank(storageDto.getAliasName()) ? storageDto.getAliasName() : storageDto.getName();
            scAliasNameMap.put(storageDto.getName(), aliasName);
        });
        // 给pod设置存储
        List<PodInfo> podInfoList = listMiddlewarePods(mw, clusterId, namespace, middlewareName, type);
        for (PodInfo pi : podInfoList) {
            // storage
            List<StorageClassDTO> scDTOList = storageClassService.fuzzySearchStorageClass(scMap, pi.getPodName());
            if (!CollectionUtils.isEmpty(scDTOList)) {
                Map<String, MiddlewareQuota> quotaMap = new HashMap<>();
                for (StorageClassDTO storageClassDTO : scDTOList) {
                    MiddlewareQuota resource = quotaMap.get(storageClassDTO.getStorageClassName());
                    if (resource == null) {
                        resource = new MiddlewareQuota();
                        resource.setStorageClassName(storageClassDTO.getStorageClassName());
                        resource.setProvisioner(storageClassDTO.getProvisioner());
                        resource.setStorageClassQuota(storageClassDTO.getStorage());
                        resource.setStorageClassQuotaValue(MathUtil.extractDigital(storageClassDTO.getStorage()) + resource.getStorageClassQuotaValue());
                        resource.setStorageClassAliasName(scAliasNameMap.get(storageClassDTO.getStorageClassName()));
                        quotaMap.put(storageClassDTO.getStorageClassName(), resource);
                    } else {
                        resource.setStorageClassQuotaValue(MathUtil.extractDigital(storageClassDTO.getStorage()) + resource.getStorageClassQuotaValue());
                        resource.getStorageClassQuota();
                    }
                }
                quotaMap.values().forEach(middlewareQuota -> {
                    middlewareQuota.setStorageClassQuota(middlewareQuota.getStorageClassQuotaValue() + MathUtil.extractUnit(middlewareQuota.getStorageClassQuota()));
                });
                pi.setStorageResources(new ArrayList<>(quotaMap.values()));
                // 给pod设置绑定的pvc
                setPodPvc(pi, pvcInfos);
            }
            // 设置pod备份状态
            setPodBackupStatus(clusterId, namespace, type, middlewareName, pi);
        }
        // 添加pod额外角色类型
        podInfoList = addPodExtraRole(clusterId, namespace, middlewareName, type, podInfoList, mw);
        // 设置pod所在可用区
        this.setPodArea(clusterId, podInfoList);
        middleware.setPodInfoGroup(convertPodListToGroup(podInfoList));
        middleware.setPods(podInfoList);
        return middleware;
    }

    @Override
    public List<PodInfo> listMiddlewarePods(String clusterId, String namespace, String middlewareName, String type) {
        MiddlewareCR middlewareCR = middlewareCRService.getCR(clusterId, namespace, type, middlewareName);
        return listMiddlewarePods(middlewareCR, clusterId, namespace, middlewareName, type);
    }

    public List<PodInfo> listMiddlewarePods(MiddlewareCR mw, String clusterId, String namespace, String middlewareName, String type) {
        List<MiddlewareInfo> pods = mw.getStatus().getInclude().get(PODS);
        List<PodInfo> podInfoList = new ArrayList<>();
        for (MiddlewareInfo po : pods) {
            Pod pod = podWrapper.get(clusterId, namespace, po.getName());
            if (pod == null) {
                continue;
            }
            PodInfo pi = convertPodInfo(pod)
                    .setRole(StringUtils.isBlank(po.getType()) ? null : po.getType().toLowerCase());
            podInfoList.add(pi);
        }
        return podInfoList;
    }



    private List<PodInfo> addPodExtraRole(String clusterId, String namespace, String middlewareName, String type, List<PodInfo> podInfoList, MiddlewareCR mw) {
        if (MiddlewareTypeEnum.REDIS.getType().equals(type)) {
            return addRedisPodExtraRole(clusterId, namespace, middlewareName, podInfoList, mw);
        }
        return podInfoList;
    }


    private List<PodInfo> addRedisPodExtraRole(String clusterId, String namespace, String middlewareName, List<PodInfo> podInfoList, MiddlewareCR mw) {
        String deployMod = RedisUtil.getRedisDeployMod(helmChartService.getInstalledValues(middlewareName, namespace, clusterService.findById(clusterId)));
        // 哨兵模式通过name判断分片，集群模式通过slave的masterNodeId判断分片
        if (deployMod.contains("sentinel")) {
            podInfoList.forEach(podInfo -> {
                String shardIndex = RedisUtil.extractShardIndex(podInfo.getPodName());
                if (StringUtils.isNotBlank(shardIndex)) {
                    podInfo.setGroup("shard-" + shardIndex);
                }
            });
        } else {
            String status = mw.getMetadata().getAnnotations().get("status");
            if (StringUtils.isNotBlank(status)) {
                Map<String, PodInfo> podInfoMap = new HashMap<>();
                podInfoList.forEach(podInfo -> {
                    podInfoMap.put(podInfo.getPodName(), podInfo);
                });

                JSONObject statusObj = JSONObject.parseObject(status);
                JSONArray conditions = statusObj.getJSONArray("conditions");
                if (CollectionUtils.isEmpty(conditions)) {
                    return podInfoList;
                }
                Map<String, String> podStatusMap = new HashMap<>();
                Map<String, String> podNodeIdMap = new HashMap<>();
                conditions.forEach(condition -> {
                    JSONObject single = (JSONObject) condition;
                    podStatusMap.put(single.getString("name"), single.getString("masterNodeId"));
                    podNodeIdMap.put(single.getString("nodeId"), single.getString("name"));
                });

                AtomicInteger groupIdIndex = new AtomicInteger();
                conditions.forEach(condition -> {
                    JSONObject single = (JSONObject) condition;
                    String podName = single.getString("name");
                    String podType = single.getString("type");
                    if ("slave".equals(podType)) {
                        String group = "shard-" + groupIdIndex;
                        PodInfo slavePodInfo = podInfoMap.get(podName);
                        slavePodInfo.setGroup(group);
                        String masterNodeId = podStatusMap.get(podName);
                        String masterPodName = podNodeIdMap.get(masterNodeId);
                        PodInfo masterPodInfo = podInfoMap.get(masterPodName);
                        masterPodInfo.setGroup(group);
                        groupIdIndex.getAndIncrement();
                    }
                });
                return new ArrayList<>(podInfoMap.values());
            }
        }
        return podInfoList;
    }
}
