package com.harmonycloud.zeus.service.k8s.impl;

import static com.harmonycloud.caas.common.constants.NameConstant.CPU;
import static com.harmonycloud.caas.common.constants.NameConstant.MEMORY;
import static com.harmonycloud.caas.common.constants.NameConstant.STORAGE;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.PERSISTENT_VOLUME_CLAIMS;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.PODS;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.model.ContainerWithStatus;
import com.harmonycloud.zeus.integration.cluster.PvcWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCRD;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRDService;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.ResourceUnitEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareQuota;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.integration.cluster.PodWrapper;
import com.harmonycloud.zeus.service.k8s.PodService;
import com.harmonycloud.tool.numeric.ResourceCalculationUtil;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Service
public class PodServiceImpl implements PodService {

    @Autowired
    private PodWrapper podWrapper;
    @Autowired
    private MiddlewareCRDService middlewareCRDService;
    @Autowired
    private PvcWrapper pvcWrapper;

    @Override
    public Middleware list(String clusterId, String namespace, String middlewareName, String type) {
        MiddlewareCRD mw = middlewareCRDService.getCR(clusterId, namespace, type, middlewareName);
        if (mw == null) {
            throw new BusinessException(DictEnum.MIDDLEWARE, middlewareName, ErrorMessage.NOT_EXIST);
        }
        Middleware middleware = middlewareCRDService.simpleConvert(mw);
        if (CollectionUtils.isEmpty(mw.getStatus().getInclude())) {
            middleware.setPods(new ArrayList<>(0));
            return middleware;
        }
        List<MiddlewareInfo> pods = mw.getStatus().getInclude().get(PODS);
        if (CollectionUtils.isEmpty(pods)) {
            middleware.setPods(new ArrayList<>(0));
            return middleware;
        }

        String storage = null;
        String storageName = null;
        List<MiddlewareInfo> pvcs = mw.getStatus().getInclude().get(PERSISTENT_VOLUME_CLAIMS);
        if (!CollectionUtils.isEmpty(pvcs)) {
            PersistentVolumeClaim pvc = pvcWrapper.get(clusterId, namespace, pvcs.get(0).getName());
            if (pvc != null) {
                storage = pvc.getSpec().getResources().getRequests().get(STORAGE).toString();
                storageName = pvc.getSpec().getStorageClassName();
            }
        }

        String finalStorage = storage;
        String finalStorageName = storageName;
        List<PodInfo> podInfoList = pods.stream().map(po -> {
            Pod pod = podWrapper.get(clusterId, namespace, po.getName());
            PodInfo pi = convertPodInfo(pod)
                    .setRole(StringUtils.isBlank(po.getType()) ? null : po.getType().toLowerCase());
            // storage
            pi.getResources().setStorageClassQuota(finalStorage).setStorageClassName(finalStorageName);
            return pi;
        }).collect(Collectors.toList());
        return middleware.setPods(podInfoList);
    }

    @Override
    public List<PodInfo> list(String clusterId, String namespace) {
        List<Pod> list = podWrapper.list(clusterId, namespace);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        
        return list.stream().map(this::convertPodInfo).collect(Collectors.toList());
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
        if ("Success".equals(pod.getStatus())) {
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
                .setRestartCount(0);
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
        ResourceRequirements resources = containers.get(0).getResources();
        resource
            .setCpu(String.valueOf(
                ResourceCalculationUtil.getResourceValue(resources.getRequests().get(CPU).toString(), CPU, "")))
            .setMemory(String.valueOf(ResourceCalculationUtil.getResourceValue(
                resources.getRequests().get(MEMORY).toString(), MEMORY, ResourceUnitEnum.GI.getUnit())))
            .setLimitCpu(String
                .valueOf(ResourceCalculationUtil.getResourceValue(resources.getLimits().get(CPU).toString(), CPU, "")))
            .setLimitMemory(String.valueOf(ResourceCalculationUtil.getResourceValue(
                resources.getLimits().get(MEMORY).toString(), MEMORY, ResourceUnitEnum.GI.getUnit())));
        return pi.setResources(resource);
    }

    @Override
    public void restart(String clusterId, String namespace, String middlewareName, String type, String podName) {
        MiddlewareCRD mw = middlewareCRDService.getCR(clusterId, namespace, type, middlewareName);
        if (mw == null) {
            throw new BusinessException(DictEnum.MIDDLEWARE, middlewareName, ErrorMessage.NOT_EXIST);
        }
        if (CollectionUtils.isEmpty(mw.getStatus().getInclude())) {
            throw new BusinessException(ErrorMessage.RESTART_POD_FAIL);
        }
        List<MiddlewareInfo> pods = mw.getStatus().getInclude().get(PODS);
        if (CollectionUtils.isEmpty(pods) || pods.stream().noneMatch(po -> podName.equals(po.getName()))) {
            throw new BusinessException(ErrorMessage.RESTART_POD_FAIL);
        }
        podWrapper.delete(clusterId, namespace, podName);
    }

}
