package com.harmonycloud.zeus.service.k8s.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.zeus.integration.cluster.MiddlewareWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.integration.cluster.bean.Status;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRService;
import com.harmonycloud.zeus.service.k8s.PodService;
import com.harmonycloud.zeus.service.middleware.MiddlewareCrTypeService;
import com.mchange.v2.util.PropertiesUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.harmonycloud.caas.common.constants.NameConstant;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.tool.date.DateUtils;

import lombok.extern.slf4j.Slf4j;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.PERSISTENT_VOLUME_CLAIMS;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.PODS;

/**
 * @author xutianhong
 * @Date 2021/4/1 5:13 下午
 */
@Service
@Slf4j
public class MiddlewareCRServiceImpl implements MiddlewareCRService {

    @Autowired
    private MiddlewareWrapper middlewareWrapper;
    @Autowired
    private PodService podService;
    @Autowired
    private MiddlewareCrTypeService middlewareCrTypeService;
    /**
     * 查询中间件列表
     *
     * @param clusterId
     *            集群id
     * @param namespace
     *            命名空间
     * @return List<Middleware>
     */
    @Override
    public List<Middleware> list(String clusterId, String namespace, String type, Boolean detail) {
        Map<String, String> label = null;
        if (StringUtils.isNotEmpty(type)) {
            label = new HashMap<>(1);
            String crType = middlewareCrTypeService.findByType(type);
            label.put("type", StringUtils.isNotEmpty(crType) ? crType : type);
        }

        List<MiddlewareCR> middlewareCRList = this.listCR(clusterId, namespace, label);
        if (CollectionUtils.isEmpty(middlewareCRList)) {
            return new ArrayList<>(0);
        }

        List<Middleware> middlewares = new ArrayList<>();
        // 封装数据
        middlewareCRList.forEach(k8sMiddleware -> {
            Middleware middleware;
            if (detail) {
                middleware = convertMiddleware(k8sMiddleware);
            } else {
                middleware = simpleConvert(k8sMiddleware);
            }
            middleware.setClusterId(clusterId);
            middlewares.add(middleware);
        });
        return middlewares;
    }

    @Override
    public List<MiddlewareCR> listCR(String clusterId, String namespace, Map<String, String> label) {
        return middlewareWrapper.list(clusterId, namespace, label);
    }

    /**
     * 查询中间件详情
     *
     * @param clusterId 集群id
     * @param namespace 命名空间
     * @param type      中间件类型
     * @param name      中间件名称
     * @return
     */
    @Override
    public Middleware simpleDetail(String clusterId, String namespace, String type, String name) {
        MiddlewareCR cr = getCR(clusterId, namespace, type, name);
        Middleware pods = podService.listPods(cr, clusterId, namespace, name, type);
        Middleware middleware = simpleConvert(cr);
        setBrokerNum(middleware, type, pods);
        middleware.setIsAllLvmStorage(pods.getIsAllLvmStorage()).setClusterId(clusterId);
        return middleware;
    }

    @Override
    public MiddlewareCR getCR(String clusterId, String namespace, String type, String name) {
        if (MiddlewareTypeEnum.isType(type)) {
            String crdName = middlewareCrTypeService.findByType(type) + "-" + name;
            return middlewareWrapper.get(clusterId, namespace, crdName);
        } else {
            List<MiddlewareCR> middlewareCRList = middlewareWrapper.list(clusterId, namespace, null);
            middlewareCRList = middlewareCRList.stream().filter(mwCRD -> mwCRD.getSpec().getName().equals(name)
                || mwCRD.getSpec().getName().equals("harmonycloud-" + name)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(middlewareCRList)) {
                throw new BusinessException(DictEnum.MIDDLEWARE, name, ErrorMessage.NOT_EXIST);
            }
            return middlewareCRList.get(0);
        }
    }

    @Override
    public MiddlewareCR getCRAndCheckRunning(Middleware middleware) {
        MiddlewareCR mw =
            getCR(middleware.getClusterId(), middleware.getNamespace(), middleware.getType(), middleware.getName());
        if (mw == null) {
            throw new BusinessException(DictEnum.MIDDLEWARE, middleware.getName(), ErrorMessage.NOT_EXIST);
        }
        if (!NameConstant.RUNNING.equalsIgnoreCase(mw.getStatus().getPhase())) {
            throw new BusinessException(ErrorMessage.MIDDLEWARE_CLUSTER_IS_NOT_RUNNING);
        }
        return mw;
    }

    /**
     * 设置broker数量
     * @param middleware
     * @param type
     * @param pods
     */
    private void setBrokerNum(Middleware middleware, String type, Middleware pods) {
        AtomicInteger brokerNum = new AtomicInteger(0);
        if ("kafka".equals(type)) {
            pods.getPods().forEach(podInfo -> {
                if (podInfo.getPodName().contains("broker")) {
                    brokerNum.getAndIncrement();
                }
            });
            KafkaDTO kafkaDTO = new KafkaDTO();
            kafkaDTO.setBrokerNum(brokerNum.get());
            middleware.setKafkaDTO(kafkaDTO);
        } else if ("rocketmq".equals(type)) {
            pods.getPods().forEach(podInfo -> {
                if (podInfo.getPodName().contains("broker")) {
                    brokerNum.getAndIncrement();
                }
            });
            RocketMQParam rocketMQParam = new RocketMQParam();
            rocketMQParam.setBrokerNum(brokerNum.get());
            middleware.setRocketMQParam(rocketMQParam);
        }
    }

    /**
     * 封装middleware
     *
     * @param k8SMiddlewareCR
     * @return Middleware
     */
    public Middleware convertMiddleware(MiddlewareCR k8SMiddlewareCR) {
        Middleware middleware = simpleConvert(k8SMiddlewareCR);
        middleware.setVersion(k8SMiddlewareCR.getMetadata().getResourceVersion());
        if (!CollectionUtils.isEmpty(k8SMiddlewareCR.getMetadata().getLabels())) {
            StringBuilder sb = new StringBuilder();
            k8SMiddlewareCR.getMetadata().getLabels()
                .forEach((k, v) -> sb.append(k).append("=").append(v).append(","));
            sb.deleteCharAt(sb.length() - 1);
            middleware.setLabels(sb.toString());
        }
        MiddlewareQuota middlewareQuota = new MiddlewareQuota();
        middlewareQuota.setNum(k8SMiddlewareCR.getStatus().getReplicas());
        if (!CollectionUtils.isEmpty(k8SMiddlewareCR.getStatus().getResources().getRequests())) {
            middlewareQuota.setCpu(k8SMiddlewareCR.getStatus().getResources().getRequests().get("cpu"));
            middlewareQuota.setMemory(k8SMiddlewareCR.getStatus().getResources().getRequests().get("memory"));
        }
        if (!CollectionUtils.isEmpty(k8SMiddlewareCR.getStatus().getResources().getLimits())) {
            middlewareQuota.setCpu(k8SMiddlewareCR.getStatus().getResources().getLimits().get("cpu"));
            middlewareQuota.setMemory(k8SMiddlewareCR.getStatus().getResources().getLimits().get("memory"));
        }
        Map<String, MiddlewareQuota> middlewareQuotaMap = new HashMap<>();
        middlewareQuotaMap.put(middleware.getType(), middlewareQuota);
        middleware.setQuota(middlewareQuotaMap);
        List<PodInfo> podInfoList = new ArrayList<>();
        Map<String, List<MiddlewareInfo>> include = k8SMiddlewareCR.getStatus().getInclude();
        if (CollectionUtils.isEmpty(include)){
            return middleware;
        }
        List<MiddlewareInfo> middlewareInfoList = k8SMiddlewareCR.getStatus().getInclude().get("pods");
        if (CollectionUtils.isEmpty(middlewareInfoList)) {
            middleware.setPods(new ArrayList<>(0));
        } else {
            middlewareInfoList.forEach(middlewareInfo -> {
                PodInfo podInfo = new PodInfo();
                podInfo.setPodName(middlewareInfo.getName());
                podInfoList.add(podInfo);
            });
            middleware.setPods(podInfoList);
        }

        return middleware;
    }

    @Override
    public Middleware simpleConvert(MiddlewareCR mw) {
        if (mw == null) {
            return null;
        }
        Middleware middleware = new Middleware()
            .setName(mw.getSpec().getName().startsWith("harmonycloud-")
                ? mw.getSpec().getName().replace("harmonycloud-", "") : mw.getSpec().getName())
            .setNamespace(mw.getMetadata().getNamespace())
            .setType(middlewareCrTypeService.findTypeByCrType(mw.getSpec().getType()))
            .setCreateTime(DateUtils.parseUTCDate(mw.getMetadata().getCreationTimestamp())).setPodNum(getPodNum(mw))
            .setPods(getPodName(mw));
        if (mw.getStatus() != null) {
            middleware.setStatus(mw.getStatus().getPhase());
            if (StringUtils.isNotEmpty(mw.getStatus().getReason()) && !"unknow".equals(mw.getStatus().getReason())) {
                middleware.setReason(mw.getStatus().getReason());
            }
        }
        return middleware;
    }

    @Override
    public boolean checkIfExist(String clusterId, String namespace, String type, String middlewareName) {
        String crdName = middlewareCrTypeService.findByType(type) + "-" + middlewareName;
        return middlewareWrapper.checkIfExist(clusterId, namespace, crdName);
    }

    @Override
    public Status getStatus(String clusterId, String namespace, String type, String middlewareName) {
        MiddlewareCR cr = getCR(clusterId, namespace, type, middlewareName);
        JSONObject statusJSON = JSONObject.parseObject(cr.getMetadata().getAnnotations().get("status"), JSONObject.class);
        return JSONObject.toJavaObject(statusJSON, Status.class);
    }

    @Override
    public List<String> getPvc(String clusterId, String namespace, String type, String name) {
        // query middleware cr
        MiddlewareCR mw = this.getCR(clusterId, namespace, type, name);
        return getPvc(mw);
    }

    @Override
    public List<String> getPvc(MiddlewareCR mw) {
        if (mw == null || mw.getStatus() == null || mw.getStatus().getInclude() == null
                || !mw.getStatus().getInclude().containsKey(PERSISTENT_VOLUME_CLAIMS)) {
            return new ArrayList<>();
        }
        List<MiddlewareInfo> pvcs = mw.getStatus().getInclude().get(PERSISTENT_VOLUME_CLAIMS);
        List<String> pvcNameList = new ArrayList<>();
        for (MiddlewareInfo pvc : pvcs) {
            pvcNameList.add(pvc.getName());
        }
        return pvcNameList;
    }

    /**
     * 获取服务pod数量
     * @param mw
     * @return
     */
    private int getPodNum(MiddlewareCR mw) {
        if ((mw.getStatus() != null && mw.getStatus().getInclude() != null && mw.getStatus().getInclude().get(PODS) != null)) {
            return mw.getStatus().getInclude().get(PODS).size();
        } else {
            return 0;
        }
    }

    /**
     * 获取pod名称
     * @param mw
     * @return List<PodInfo>
     */
    private List<PodInfo> getPodName(MiddlewareCR mw) {
        if (mw.getStatus() != null && mw.getStatus().getInclude() != null && mw.getStatus().getInclude().containsKey("pods")) {
            return mw.getStatus().getInclude().get("pods").stream().map(pod -> {
                PodInfo podInfo = new PodInfo();
                podInfo.setPodName(pod.getName());
                return podInfo;
            }).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
