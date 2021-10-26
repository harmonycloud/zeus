package com.harmonycloud.zeus.service.k8s.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCRD;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRDService;
import com.harmonycloud.zeus.integration.cluster.NamespaceWrapper;
import com.harmonycloud.zeus.service.k8s.NamespaceService;
import com.harmonycloud.zeus.service.k8s.ResourceQuotaService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.caas.common.model.middleware.ResourceQuotaDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Slf4j
@Service
public class NamespaceServiceImpl implements NamespaceService {

    private static final Set<String> protectNamespaceList = new HashSet<>();
    private static String labelKey = null;
    private static String labelValue = null;

    @Autowired
    private NamespaceWrapper namespaceWrapper;
    @Autowired
    private ResourceQuotaService resourceQuotaService;
    @Autowired
    private MiddlewareCRDService middlewareCRDService;
    
    @Value("${k8s.namespace.protect:default,kube-system,kube-public,cluster-top,cicd,caas-system,kube-federation-system,harbor-system,logging,monitoring,velero,middleware-system}")
    private void setProtectNamespaceList(String protectNamespaces) {
        protectNamespaceList.addAll(Arrays.asList(protectNamespaces.split(",")));
    }

    @Value("${k8s.namespace.label:middleware=true}")
    private void setLabel(String l) {
        String[] labelArr = l.split("=");
        labelKey = labelArr[0];
        labelValue = labelArr[0];
    }

    @Override
    public List<Namespace> list(String clusterId) {
        return list(clusterId, false, null);
    }

    @Override
    public List<Namespace> list(String clusterId, boolean all, String keyword) {
        return list(clusterId, all, false, false, keyword);
    }

    @Override
    public List<Namespace> list(String clusterId, boolean all, boolean withQuota, boolean withMiddleware, String keyword) {
        List<io.fabric8.kubernetes.api.model.Namespace> nsList = namespaceWrapper.list(clusterId);
        List<Namespace> list = nsList.stream()
            .filter(ns -> (all || ns.getMetadata().getLabels() != null
                && StringUtils.equals(ns.getMetadata().getLabels().get(labelKey), labelValue))
                && !protectNamespaceList.contains(ns.getMetadata().getName())
                && (StringUtils.isBlank(keyword) || ns.getMetadata().getName().contains(keyword)))
            .map(ns -> {
                Namespace namespace = new Namespace().setName(ns.getMetadata().getName()).setClusterId(clusterId);
                // 昵称
                if (ns.getMetadata().getAnnotations() != null
                    && ns.getMetadata().getAnnotations().containsKey("aliasName")) {
                    namespace.setAliasName(ns.getMetadata().getAnnotations().get("aliasName"));
                }
                // 是否已注册
                namespace.setRegistered(ns.getMetadata().getLabels() != null
                    && StringUtils.equals(ns.getMetadata().getLabels().get(labelKey), labelValue));
                return namespace;
            }).collect(Collectors.toList());

        // 返回其他信息
        if (withQuota || withMiddleware) {
            // 命名空间配额
            Map<String, ResourceQuotaDTO> rqMap = null;
            if (withQuota) {
                List<ResourceQuotaDTO> rqDtoList = resourceQuotaService.list(clusterId);
                if (!CollectionUtils.isEmpty(rqDtoList)) {
                    rqMap = rqDtoList.stream().collect(Collectors.toMap(ResourceQuotaDTO::getNamespace, dto -> dto));
                }
            }
            // 中间件实例信息
            Map<String, List<MiddlewareCRD>> mwMap = null;
            if (withMiddleware) {
                List<MiddlewareCRD> middlewares = middlewareCRDService.listCR(clusterId, null, null);
                if (!CollectionUtils.isEmpty(middlewares)) {
                    mwMap = middlewares.stream().collect(Collectors.groupingBy(mw -> mw.getMetadata().getNamespace()));
                }
            }
            // 遍历赋值
            if (rqMap != null || mwMap != null) {
                for (Namespace ns : list) {
                    if (rqMap != null && rqMap.get(ns.getName()) != null) {
                        ns.setQuotas(rqMap.get(ns.getName()).getQuotas());
                    }
                    if (mwMap != null && mwMap.get(ns.getName()) != null) {
                        ns.setMiddlewareReplicas(mwMap.get(ns.getName()).size());
                    }
                }
            }
        }

        return list;
    }

    @Override
    public List<String> registry(String clusterId, List<String> namespaceList) {
        // 查出所有命名空间
        List<io.fabric8.kubernetes.api.model.Namespace> allNsList = namespaceWrapper.list(clusterId);
        List<String> existNsList = new ArrayList<>();
        Map<String, io.fabric8.kubernetes.api.model.Namespace> existNsMap = new HashMap<>(allNsList.size());

        Map<String, io.fabric8.kubernetes.api.model.Namespace> allNsMap = allNsList.stream().peek(ns -> {
            if (!CollectionUtils.isEmpty(ns.getMetadata().getLabels())
                && StringUtils.equals(ns.getMetadata().getLabels().get(labelKey), labelValue)) {
                existNsList.add(ns.getMetadata().getName());
                existNsMap.put(ns.getMetadata().getName(), ns);
            }
        }).collect(Collectors.toMap(ns -> ns.getMetadata().getName(), ns -> ns));

        List<String> failNsList = new ArrayList<>();

        // 注销命名空间
        List<String> cancelNsList = new ArrayList<>(existNsList);
        cancelNsList.removeAll(namespaceList);
        cancelNsList.forEach(nsName -> {
            io.fabric8.kubernetes.api.model.Namespace ns = existNsMap.get(nsName);
            ns.getMetadata().getLabels().remove(labelKey);
            try {
                namespaceWrapper.save(clusterId, ns);
            } catch (Exception e) {
                log.error("集群：{}，命名空间：{}，注销失败", clusterId, ns.getMetadata().getName(), e);
                failNsList.add(ns.getMetadata().getName());
            }
        });

        // 注册命名空间，需要加上label
        namespaceList.removeAll(existNsList);
        namespaceList.forEach(nsName -> {
            io.fabric8.kubernetes.api.model.Namespace ns = allNsMap.get(nsName);
            if (ns == null || protectNamespaceList.contains(nsName)) {
                return;
            }
            if (ns.getMetadata().getLabels() == null) {
                ns.getMetadata().setLabels(new HashMap<>());
            }
            ns.getMetadata().getLabels().put(labelKey, labelValue);
            try {
                namespaceWrapper.save(clusterId, ns);
            } catch (Exception e) {
                log.error("集群：{}，命名空间：{}，注册失败", clusterId, ns.getMetadata().getName(), e);
                failNsList.add(ns.getMetadata().getName());
            }
        });

        return failNsList;
    }

    @Override
    public void save(String clusterId, String name, Map<String, String> label) {
        // 创建namespace
        io.fabric8.kubernetes.api.model.Namespace ns = new io.fabric8.kubernetes.api.model.Namespace();
        if (CollectionUtils.isEmpty(label)){
            label = new HashMap<>(1);
        }
        ObjectMeta meta = new ObjectMeta();
        meta.setName(name);
        meta.setLabels(label);
        ns.setMetadata(meta);
        namespaceWrapper.save(clusterId, ns);
    }
}
