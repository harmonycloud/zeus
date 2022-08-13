package com.harmonycloud.zeus.service.k8s.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.constants.NamespaceConstant;
import com.harmonycloud.zeus.bean.user.BeanProjectNamespace;
import com.harmonycloud.zeus.dao.user.BeanProjectNamespaceMapper;
import com.harmonycloud.zeus.service.user.ProjectService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.caas.common.model.middleware.ResourceQuotaDTO;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.zeus.bean.user.BeanProjectNamespace;
import com.harmonycloud.zeus.dao.user.BeanProjectNamespaceMapper;
import com.harmonycloud.zeus.integration.cluster.NamespaceWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.service.k8s.AbstractNamespaceService;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRService;
import com.harmonycloud.zeus.service.k8s.NamespaceService;
import com.harmonycloud.zeus.service.k8s.ResourceQuotaService;
import com.harmonycloud.zeus.service.user.ProjectService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import com.harmonycloud.caas.common.constants.NamespaceConstant;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.util.ObjectUtils;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_OPERATOR;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "system.usercenter", havingValue = "zeus")
public class NamespaceServiceImpl implements NamespaceService {

    private static final Set<String> protectNamespaceList = new HashSet<>();
    private static String labelKey = null;
    private static String labelValue = null;

    private static final String KEY_NAMESPACE_CHINESE = "alias_name";

    @Autowired
    private NamespaceWrapper namespaceWrapper;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private BeanProjectNamespaceMapper beanProjectNamespaceMapper;
    @Autowired
    public MiddlewareCRService middlewareCRService;
    @Autowired
    private ResourceQuotaService resourceQuotaService;

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
    public Namespace get(String clusterId, String namespace) {
        io.fabric8.kubernetes.api.model.Namespace ns = namespaceWrapper.get(clusterId, namespace);
        return convertNamespace(clusterId, ns);
    }

    @Override
    public List<Namespace> list(String clusterId) {
        return list(clusterId, false, null);
    }

    @Override
    public List<Namespace> list(String clusterId, boolean all, String keyword) {
        return list(clusterId, all, false, false, keyword, null);
    }

    @Override
    public List<Namespace> list(String clusterId, boolean all, boolean withQuota, boolean withMiddleware,
                                String keyword, String projectId) {
        List<io.fabric8.kubernetes.api.model.Namespace> nsList = namespaceWrapper.list(clusterId);
        List<Namespace> list = nsList.stream()
                .filter(ns -> (all || ns.getMetadata().getLabels() != null
                        && StringUtils.equals(ns.getMetadata().getLabels().get(labelKey), labelValue))
                        && !protectNamespaceList.contains(ns.getMetadata().getName())
                        && (StringUtils.isBlank(keyword) || (ns.getMetadata().getAnnotations() != null
                        && ns.getMetadata().getAnnotations().containsKey(KEY_NAMESPACE_CHINESE)
                        && ns.getMetadata().getAnnotations().get(KEY_NAMESPACE_CHINESE).contains(keyword))))
                .map(ns -> convertNamespace(clusterId, ns)).collect(Collectors.toList());

        if (StringUtils.isNotEmpty(projectId)) {
            List<Namespace> alNsList = projectService.getNamespace(projectId).stream()
                    .filter(ns -> ns.getClusterId().equals(clusterId)).collect(Collectors.toList());
            list = list.stream().filter(ns -> alNsList.stream().anyMatch(alNs -> alNs.getName().equals(ns.getName())))
                    .collect(Collectors.toList());
        }

        if (withQuota) {
            listNamespaceWithQuota(list, clusterId);
        }
        if (withMiddleware) {
            listNamespaceWithMiddleware(list, clusterId);
        }

        return list;
    }

    @Override
    public void save(Namespace namespace, Map<String, String> label, Boolean exist) {
        if (exist && checkExist(namespace.getClusterId(), namespace.getName())) {
            throw new BusinessException(ErrorMessage.NAMESPACE_EXIST);
        }
        // 判断中文名是否重复
        if (exist && checkAliasNameExist(namespace.getClusterId(), namespace.getAliasName())){
            throw new BusinessException(ErrorMessage.NAMESPACE_ALIAS_NAME_EXIST);
        }
        Map<String, String> annotations = new HashMap<>();
        if (StringUtils.isNotEmpty(namespace.getAliasName())) {
            annotations.put("alias_name", namespace.getAliasName());
        }
        if (StringUtils.isNotEmpty(namespace.getProjectId())) {
            annotations.put("project_id", namespace.getProjectId());
            projectService.bindNamespace(namespace);
        }
        save(namespace.getClusterId(), namespace.getName(), label, annotations);
    }

    @Override
    public void save(String clusterId, String name, Map<String, String> label, Map<String, String> annotations) {
        // 创建namespace
        io.fabric8.kubernetes.api.model.Namespace ns = new io.fabric8.kubernetes.api.model.Namespace();
        if (CollectionUtils.isEmpty(label)) {
            label = new HashMap<>(1);
        }
        ObjectMeta meta = new ObjectMeta();
        meta.setName(name);
        meta.setLabels(label);
        meta.setAnnotations(annotations);
        ns.setMetadata(meta);
        namespaceWrapper.save(clusterId, ns);
    }

    @Override
    public void delete(String clusterId, String name) {
        if (MIDDLEWARE_OPERATOR.equals(name)) {
            throw new BusinessException(ErrorMessage.CAN_NOT_DELETE_NS_MIDDLEWARE_OPERATOR);
        }
        // 判断是否存在中间件
        List<MiddlewareCR> middlewareCRList = middlewareCRService.listCR(clusterId, name, null);
        if (!CollectionUtils.isEmpty(middlewareCRList)) {
            throw new BusinessException(ErrorMessage.NAMESPACE_NOT_EMPTY);
        }
        io.fabric8.kubernetes.api.model.Namespace ns = new io.fabric8.kubernetes.api.model.Namespace();
        ObjectMeta meta = new ObjectMeta();
        meta.setName(name);
        ns.setMetadata(meta);
        namespaceWrapper.delete(clusterId, ns);
        // 解绑分区
        projectService.unBindNamespace(null, clusterId, name);
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
            // 校验是否绑定项目
            QueryWrapper<BeanProjectNamespace> wrapper = new QueryWrapper<BeanProjectNamespace>().eq("namespace", name);
            List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);
            if (!CollectionUtils.isEmpty(beanProjectNamespaceList)) {
                throw new BusinessException(ErrorMessage.PROJECT_NAMESPACE_ALREADY_BIND);
            }
            ns.getMetadata().getLabels().remove(labelKey);
        }
        try {
            namespaceWrapper.save(clusterId, ns);
        } catch (Exception e) {
            log.error("分区{}  注册失败", name);
            throw new BusinessException(ErrorMessage.NAMESPACE_REGISTRY_FAILED);
        }
    }

    @Override
    public void createMiddlewareOperator(String clusterId) {
        synchronized (this) {
            List<Namespace> namespaceList = list(clusterId, false, "middleware-operator");
            // 检验分区是否存在
            if (CollectionUtils.isEmpty(namespaceList)) {
                Map<String, String> label = new HashMap<>();
                label.put("middleware", "middleware");
                save(clusterId, "middleware-operator", label, null);
            }
        }
    }

    @Override
    public void updateAvailableDomain(String clusterId, String name, boolean availableDomain) {
        io.fabric8.kubernetes.api.model.Namespace namespace = namespaceWrapper.get(clusterId, name);
        Map<String, String> labels = namespace.getMetadata().getLabels();
        labels.put(NamespaceConstant.KEY_AVAILABLE_DOMAIN, String.valueOf(availableDomain));
        namespaceWrapper.save(clusterId, namespace);
    }

    @Override
    public void setAvailableDomain(String clusterId, String name, io.fabric8.kubernetes.api.model.Namespace originalNamespace, Namespace namespace) {
        if (originalNamespace == null) {
            originalNamespace = namespaceWrapper.get(clusterId, name);
        }
        if (originalNamespace != null && originalNamespace.getMetadata() != null && originalNamespace.getMetadata().getLabels() != null
                && originalNamespace.getMetadata().getLabels().containsKey(NamespaceConstant.KEY_AVAILABLE_DOMAIN)) {
            namespace.setAvailableDomain(Boolean.parseBoolean(originalNamespace.getMetadata().getLabels().get(NamespaceConstant.KEY_AVAILABLE_DOMAIN)));
        } else {
            namespace.setAvailableDomain(false);
        }
    }

    @Override
    public boolean checkAvailableDomain(String clusterId, String name) {
        io.fabric8.kubernetes.api.model.Namespace namespace = namespaceWrapper.get(clusterId, name);
        if (namespace == null || namespace.getMetadata().getLabels() == null || (!namespace.getMetadata().getLabels().containsKey(NamespaceConstant.KEY_AVAILABLE_DOMAIN))) {
            return false;
        }
        return Boolean.parseBoolean(namespace.getMetadata().getLabels().get(NamespaceConstant.KEY_AVAILABLE_DOMAIN));
    }

    public boolean checkExist(String clusterId, String name) {
        List<io.fabric8.kubernetes.api.model.Namespace> nsList = namespaceWrapper.list(clusterId);
        return nsList.stream().anyMatch(ns -> ns.getMetadata().getName().equals(name));
    }

    public Namespace convertNamespace(String clusterId, io.fabric8.kubernetes.api.model.Namespace ns) {
        Namespace namespace = new Namespace().setName(ns.getMetadata().getName()).setClusterId(clusterId);
        // 昵称
        if (ns.getMetadata().getAnnotations() != null
                && ns.getMetadata().getAnnotations().containsKey(KEY_NAMESPACE_CHINESE)) {
            namespace.setAliasName(ns.getMetadata().getAnnotations().get(KEY_NAMESPACE_CHINESE));
        }
        // 是否已注册
        namespace.setRegistered(ns.getMetadata().getLabels() != null
                && StringUtils.equals(ns.getMetadata().getLabels().get(labelKey), labelValue));
        // 创建时间
        namespace.setCreateTime(
                DateUtils.parseDate(ns.getMetadata().getCreationTimestamp(), DateUtils.YYYY_MM_DD_T_HH_MM_SS_Z));
        // 状态
        namespace.setPhase(ns.getStatus().getPhase());
        this.setAvailableDomain(clusterId, namespace.getName(), ns, namespace);
        return namespace;
    }

    @Override
    public List<Namespace> listNamespaceWithQuota(List<Namespace> namespaces, String clusterId) {
        Map<String, ResourceQuotaDTO> rqMap = null;
        List<ResourceQuotaDTO> rqDtoList = resourceQuotaService.list(clusterId);
        if (!CollectionUtils.isEmpty(rqDtoList)) {
            rqMap = rqDtoList.stream().collect(Collectors.toMap(ResourceQuotaDTO::getNamespace, dto -> dto));
        }
        for (Namespace ns : namespaces) {
            if (rqMap != null && rqMap.get(ns.getName()) != null) {
                ns.setQuotas(rqMap.get(ns.getName()).getQuotas());
            }
        }
        return namespaces;
    }

    @Override
    public List<Namespace> listNamespaceWithMiddleware(List<Namespace> namespaces, String clusterId) {
        // 中间件实例信息
        Map<String, List<MiddlewareCR>> mwMap = null;
        List<MiddlewareCR> middlewares = middlewareCRService.listCR(clusterId, null, null);
        if (!CollectionUtils.isEmpty(middlewares)) {
            mwMap = middlewares.stream().collect(Collectors.groupingBy(mw -> mw.getMetadata().getNamespace()));
        }
        for (Namespace ns : namespaces) {
            if (mwMap != null && mwMap.get(ns.getName()) != null) {
                ns.setMiddlewareReplicas(mwMap.get(ns.getName()).size());
            }
        }
        return namespaces;
    }


    public boolean checkAliasNameExist(String clusterId, String aliasName) {
        List<io.fabric8.kubernetes.api.model.Namespace> nsList = namespaceWrapper.list(clusterId);
        for (io.fabric8.kubernetes.api.model.Namespace ns : nsList) {
            if (ns.getMetadata().getAnnotations() != null && ns.getMetadata().getAnnotations().containsKey("alias_name")
                    && aliasName.equals(ns.getMetadata().getAnnotations().get("alias_name"))) {
                return true;
            }
        }
        return false;
    }

}
