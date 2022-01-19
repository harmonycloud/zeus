package com.harmonycloud.zeus.service.k8s.impl;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.caas.common.model.middleware.ResourceQuotaDTO;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import com.harmonycloud.caas.filters.user.CurrentUser;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.zeus.integration.cluster.NamespaceWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCRD;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRDService;
import com.harmonycloud.zeus.service.k8s.NamespaceService;
import com.harmonycloud.zeus.service.k8s.ResourceQuotaService;
import com.harmonycloud.zeus.service.user.ClusterRoleService;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_OPERATOR;

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
    @Autowired
    private ClusterRoleService clusterRoleService;

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
                // 创建时间
                namespace.setCreateTime(
                    DateUtils.parseDate(ns.getMetadata().getCreationTimestamp(), DateUtils.YYYY_MM_DD_T_HH_MM_SS_Z));
                return namespace;
            }).collect(Collectors.toList());

        // 根据用户角色集群分区权限进行过滤
        CurrentUser currentUser = CurrentUserRepository.getUserExistNull();
        if (!ObjectUtils.isEmpty(currentUser)) {
            JSONObject role = JwtTokenComponent.checkToken(currentUser.getToken()).getValue();
            if (!"超级管理员".equals(role.getString("roleName"))) {
                MiddlewareClusterDTO cluster = clusterRoleService.get(role.getInteger("roleId"), clusterId);
                list = list.stream()
                    .filter(
                        ns -> cluster.getNamespaceList().stream().anyMatch(cns -> cns.getName().equals(ns.getName())))
                    .collect(Collectors.toList());
            }
        }

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
    public void save(String clusterId, String name, Map<String, String> label, Boolean exist) {
        if (exist && checkExist(clusterId, name)){
            throw new BusinessException(ErrorMessage.NAMESPACE_EXIST);
        }
        save(clusterId, name, label);
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

    @Override
    public void delete(String clusterId, String name) {
        if (MIDDLEWARE_OPERATOR.equals(name)){
            throw new BusinessException(ErrorMessage.CAN_NOT_DELETE_NS_MIDDLEWARE_OPERATOR);
        }
        io.fabric8.kubernetes.api.model.Namespace ns = new io.fabric8.kubernetes.api.model.Namespace();
        ObjectMeta meta = new ObjectMeta();
        meta.setName(name);
        ns.setMetadata(meta);
        namespaceWrapper.delete(clusterId, ns);
    }

    @Override
    public void registry(String clusterId, String name, Boolean registered) {
        List<io.fabric8.kubernetes.api.model.Namespace> nsList = namespaceWrapper.list(clusterId).stream()
            .filter(ns -> ns.getMetadata().getName().equals(name)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(nsList)) {
            throw new BusinessException(ErrorMessage.NAMESPACE_NOT_FOUND);
        }
        io.fabric8.kubernetes.api.model.Namespace ns = nsList.get(0);
        if (registered) {
            if (ns.getMetadata().getLabels() == null) {
                ns.getMetadata().setLabels(new HashMap<>());
            }
            ns.getMetadata().getLabels().put(labelKey, labelValue);
        } else {
            ns.getMetadata().getLabels().remove(labelKey);
        }
        try {
            namespaceWrapper.save(clusterId, ns);
        } catch (Exception e) {
            log.error("分区{}  注册失败", name);
            throw new BusinessException(ErrorMessage.NAMESPACE_REGISTRY_FAILED);
        }
    }

    public boolean checkExist(String clusterId, String name) {
        List<io.fabric8.kubernetes.api.model.Namespace> nsList = namespaceWrapper.list(clusterId);
        return nsList.stream().anyMatch(ns -> ns.getMetadata().getName().equals(name));
    }
}
