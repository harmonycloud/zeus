package com.middleware.zeus.service.k8s.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.common.constants.NamespaceConstant;
import com.middleware.zeus.common.model.*;
import com.middleware.zeus.common.model.middleware.ImageRepositoryDTO;
import com.middleware.zeus.common.model.middleware.StorageClassInfo;
import com.middleware.zeus.common.model.user.ProjectNamespaceDo;
import com.middleware.zeus.bean.user.BeanProjectNamespace;
import com.middleware.zeus.dao.user.BeanProjectNamespaceMapper;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.service.middleware.ImageRepositoryService;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.util.ThreadPoolExecutorFactory;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.Na;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.middleware.Namespace;
import com.middleware.zeus.common.model.middleware.ResourceQuotaDTO;
import com.middleware.zeus.util.date.DateUtils;
import com.middleware.zeus.integration.cluster.NamespaceWrapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_OPERATOR;

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
    @Autowired
    private StorageService storageService;
    @Autowired
    private ServiceAccountService serviceAccountService;
    @Autowired
    private ImageRepositoryService imageRepositoryService;

    @Value("${system.privateRegistry.middlewareServiceAccount:default}")
    private String middlewareServiceAccount;
    @Value("${system.privateRegistry.updateNamespaceDefaultSecret:false}")
    private boolean updateNamespaceDefaultSecret;

    @Value("${k8s.namespace.protect:default,kube-system,kube-public,cluster-top,cicd,caas-system,kube-federation-system,harbor-system,logging,monitoring,velero,middleware-system}")
    private void setProtectNamespaceList(String protectNamespaces) {
        protectNamespaceList.addAll(Arrays.asList(protectNamespaces.split(",")));
    }

    @Override
    public boolean isNamespacceProtected(String namespace) {
        return protectNamespaceList.contains(namespace);
    }

    @Value("${k8s.namespace.label:middleware=middleware}")
    private void setLabel(String l) {
        String[] labelArr = l.split("=");
        labelKey = labelArr[0];
        labelValue = labelArr[1];
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
        return list(clusterId, all, false, false, keyword, null, null);
    }

    @Override
    public List<Namespace> list(String clusterId, boolean all, boolean withQuota, boolean withMiddleware,
        String keyword, String organId, String projectId) {
        List<io.fabric8.kubernetes.api.model.Namespace> nsList = namespaceWrapper.list(clusterId);
        List<Namespace> list = nsList.stream()
            .filter(ns -> (all || ns.getMetadata().getLabels() != null
                && StringUtils.equals(ns.getMetadata().getLabels().get(labelKey), labelValue))
                && !protectNamespaceList.contains(ns.getMetadata().getName())
                && (StringUtils.isEmpty(keyword) || (ns.getMetadata().getAnnotations() != null
                    && ns.getMetadata().getAnnotations().containsKey(KEY_NAMESPACE_CHINESE)
                    && ns.getMetadata().getAnnotations().get(KEY_NAMESPACE_CHINESE).contains(keyword)
                    || (ns.getMetadata().getName().contains(keyword)))))
            .map(ns -> convertNamespace(clusterId, ns)).collect(Collectors.toList());

        if (StringUtils.isNotEmpty(projectId)) {
            List<Namespace> alNsList = projectService.getNamespace(organId, projectId).stream()
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

        // 设置分区所属项目
        List<ProjectNamespaceDo> projectNamespaceList = projectService.listNamespace(clusterId);
        Map<String, ProjectNamespaceDo> projectNamespaceMap = projectNamespaceList.stream()
            .collect(Collectors.toMap(ProjectNamespaceDo::getNamespace, projectNamespaceDo -> projectNamespaceDo));
        list.forEach(ns -> {
            if (projectNamespaceMap.containsKey(ns.getName())){
                ns.setOrganId(projectNamespaceMap.get(ns.getName()).getOrganId());
                ns.setProjectId(projectNamespaceMap.get(ns.getName()).getProjectId());
                ns.setProjectName(projectNamespaceMap.get(ns.getName()).getProjectName());
            }
        });

        return list;
    }

    @Override
    public void save(Namespace namespace, Map<String, String> label, Boolean exist) {
        if (exist && checkExist(namespace.getClusterId(), namespace.getName())) {
            throw new BusinessException(ErrorMessage.NAMESPACE_EXIST);
        }
        // 判断中文名是否重复
        if (exist && onlyCheckAliasNameExist(namespace.getClusterId(), namespace.getAliasName())){
            throw new BusinessException(ErrorMessage.NAMESPACE_ALIAS_NAME_EXIST);
        }
        // create ns
        Map<String, String> annotations = new HashMap<>();
        if (StringUtils.isNotEmpty(namespace.getAliasName())) {
            annotations.put("alias_name", namespace.getAliasName());
        }
        // set ns uid range
        if (namespace.getContainerUIDRange() != null) {
            putContainerIdentityRange(namespace, annotations);
        }
        save(namespace.getClusterId(), namespace.getName(), label, annotations);
        if (updateNamespaceDefaultSecret) {
            // bind imagePullSecret to default sa
            bindImagePullSecret(namespace.getClusterId(), namespace.getName(), 1500);
        }
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
        if (updateNamespaceDefaultSecret) {
            // bind imagePullSecret to default sa
            bindImagePullSecret(clusterId, name, 1);
        }
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
        // 解绑分区
        projectService.unBindNamespace(null, null, clusterId, name);
        // 更新分区接入信息
        update(clusterId, name, new Namespace().setRegistered(false));
        // 删除分区
        io.fabric8.kubernetes.api.model.Namespace ns = new io.fabric8.kubernetes.api.model.Namespace();
        ObjectMeta meta = new ObjectMeta();
        meta.setName(name);
        ns.setMetadata(meta);
        namespaceWrapper.delete(clusterId, ns);
    }

    @Override
    public void update(String clusterId, String name, Namespace namespace) {
        // 修改中文名
        io.fabric8.kubernetes.api.model.Namespace ns = namespaceWrapper.get(clusterId, name);
        if (ns == null) {
            throw new BusinessException(ErrorMessage.NAMESPACE_NOT_FOUND);
        }
        if (ns.getMetadata().getAnnotations() == null) {
            ns.getMetadata().setAnnotations(new HashMap<>());
        }
        if (StringUtils.isNotEmpty(namespace.getAliasName())) {
            ns.getMetadata().getAnnotations().put("alias_name", namespace.getAliasName());
        }
        // 修改分区注册状态
        if (namespace.getRegistered() != null) {
            register(clusterId, name, namespace.getRegistered(), ns);
        }
        // 修改资源配额
        if (namespace.getQuotas() != null) {
            resourceQuotaService.update(clusterId, name, namespace.getQuotas());
        }
        // 修改分区uid
        if(namespace.getContainerUIDRange() != null){
            putContainerIdentityRange(namespace, ns.getMetadata().getAnnotations());
        }
        namespaceWrapper.save(clusterId, ns);
        // 修改数据表 project_namespace 中分区中文名
        updateAliasName(clusterId, name, namespace.getAliasName());
        // 给分区添加imagepullsecret
        ThreadPoolExecutorFactory.executor.execute(()-> bindImagePullSecret(clusterId, name));
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
    public void bindProject(String clusterId, String name, String aliasName, String organId, String projectId) {
        if (StringUtils.isNotEmpty(organId) && StringUtils.isNotEmpty(projectId)){
            Namespace namespace = new Namespace();
            namespace.setClusterId(clusterId).setName(name).setAliasName(aliasName).setOrganId(organId).setProjectId(projectId);
            // 先解除现有绑定
            projectService.unBindNamespace(null, null, clusterId, name);
            // 重新绑定
            projectService.bindNamespace(namespace);
        }else {
            projectService.unBindNamespace(null, null, clusterId, name);
        }
    }

    @Override
    public ResourceQuotaDo cpuMemory(String clusterId, String name) {
        // 查询命名空间信息
        Namespace namespace = this.get(clusterId, name);
        // 获取资源使用情况
        ResourceQuotaDo resourceQuotaDo = resourceQuotaService.get(clusterId, name, name + "quota");
        // 设置双活信息
        resourceQuotaDo.setAvailableDomain(namespace.isAvailableDomain());
        return resourceQuotaDo;
    }

    @Override
    public List<StorageDto> storage(String clusterId, String namespace) {
        // 查询已接入的存储服务
        List<StorageDto> storageDtoList = storageService.list(clusterId, null, null, false);
        if (CollectionUtils.isEmpty(storageDtoList)){
            return new ArrayList<>();
        }
        ResourceQuotaDo resourceQuotaDo = resourceQuotaService.get(clusterId, namespace, namespace + "quota");
        Map<String, QuotaBase> storageQuotaMap = resourceQuotaDo.getStorageList().stream().collect(Collectors.toMap(StorageQuota::getName, StorageQuota::getStorage));
        if (CollectionUtils.isEmpty(storageDtoList)){
            return new ArrayList<>();
        }

        storageDtoList = storageDtoList.stream()
            .filter(storageDto -> storageDto.getStorageClassList().stream()
                .anyMatch(storageClassInfo -> storageQuotaMap.containsKey(storageClassInfo.getName())))
            .collect(Collectors.toList());

        for (StorageDto storageDto : storageDtoList) {
            double requestStorage = 0.0;
            double usedStorage = 0.0;
            for (StorageClassInfo sc : storageDto.getStorageClassList()) {
                if (storageQuotaMap.containsKey(sc.getName())) {
                    QuotaBase quotaBase = storageQuotaMap.get(sc.getName());
                    if (quotaBase.getRequest() != null) {
                        if (requestStorage == 0.0) {
                            requestStorage += quotaBase.getRequest();
                        } else {
                            requestStorage =
                                requestStorage < quotaBase.getRequest() ? requestStorage : quotaBase.getRequest();
                        }
                    }
                    if (quotaBase.getUsed() != null) {
                        if (usedStorage == 0.0) {
                            usedStorage += quotaBase.getUsed();
                        } else {
                            usedStorage = usedStorage > quotaBase.getUsed() ? usedStorage : quotaBase.getUsed();
                        }
                    }
                }
            }
            storageDto.setQuota(new QuotaBase().setRequest(requestStorage).setUsed(usedStorage));
        }

        return storageDtoList;
    }

    @Override
    public void updateAvailableDomain(String clusterId, String name, boolean availableDomain) {
        io.fabric8.kubernetes.api.model.Namespace namespace = namespaceWrapper.get(clusterId, name);
        if (namespace.getMetadata().getLabels() == null) {
            namespace.getMetadata().setLabels(new HashMap<>());
        }
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
    public boolean isOpenAvailableDomain(String clusterId, String name) {
        io.fabric8.kubernetes.api.model.Namespace namespace = namespaceWrapper.get(clusterId, name);
        if (namespace == null || namespace.getMetadata().getLabels() == null || (!namespace.getMetadata().getLabels().containsKey(NamespaceConstant.KEY_AVAILABLE_DOMAIN))) {
            return false;
        }
        return Boolean.parseBoolean(namespace.getMetadata().getLabels().get(NamespaceConstant.KEY_AVAILABLE_DOMAIN));
    }

    /**
     * 等待指定时间后(毫秒),为每个镜像仓库创建secret(如果未创建)，并绑定到分区imagepullsecret(如果未绑定)
     * @param clusterId
     * @param namespace
     */
    private void bindImagePullSecret(String clusterId, String namespace, Integer waitMilliSeconds) {
        if (waitMilliSeconds == null) {
            waitMilliSeconds = 1000;
        }
        try {
            Thread.sleep(waitMilliSeconds);
        } catch (InterruptedException e) {
            log.error("线程等待异常");
        }
        ThreadPoolExecutorFactory.executor.execute(()-> checkAndBindImagePullSecret(clusterId, namespace, null));
    }

    /**
     * 为镜像仓库创建secret(如果未创建)，并绑定到分区imagepullsecret(如果未绑定)
     * @param clusterId
     * @param namespace
     */
    private void bindImagePullSecret(String clusterId, String namespace){
        checkAndBindImagePullSecret(clusterId, namespace, null);
    }

    @Override
    public void checkAndBindImagePullSecret(String clusterId, String namespace, Integer repositoryId) {
        ServiceAccount serviceAccount = serviceAccountService.get(clusterId, namespace, middlewareServiceAccount);
        List<LocalObjectReference> saImagePullSecrets = serviceAccount.getImagePullSecrets();

        List<ImageRepositoryDTO> imageRepositoryDTOS = imageRepositoryService.list(clusterId);
        if (repositoryId != null) {
            imageRepositoryDTOS = imageRepositoryDTOS.stream().filter(imageRepositoryDTO ->
                    repositoryId.equals(imageRepositoryDTO.getId())).collect(Collectors.toList());
        }

        if (!CollectionUtils.isEmpty(imageRepositoryDTOS)) {
            // 遍历集群所有镜像仓库，为每个镜像仓库创建secret(如果该镜像仓库不存在secret)
            for (ImageRepositoryDTO imageRepositoryDTO : imageRepositoryDTOS) {
                Integer currentRepositoryId = imageRepositoryDTO.getId();
                io.fabric8.kubernetes.api.model.Secret secret = imageRepositoryService.
                        getImagePullSecret(clusterId, namespace, String.valueOf(currentRepositoryId));
                if (secret == null) {
                    imageRepositoryService.createOrReplaceImagePullSecret(clusterId, namespace, currentRepositoryId);
                }
            }
            // 等待secret创建完成
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 将镜像仓库secret绑定到分区sa default
            List<Secret> nsImagePullSecrets = imageRepositoryService.listImagePullSecret(clusterId, namespace);
            if (!CollectionUtils.isEmpty(saImagePullSecrets)) {
                List<String> secretNameSet = saImagePullSecrets.stream().map(LocalObjectReference::getName).collect(Collectors.toList());
                nsImagePullSecrets = nsImagePullSecrets.stream().filter(imgSecret ->
                        secretNameSet.contains(imgSecret.getMetadata().getName())).collect(Collectors.toList());
            }
            // 将secret绑定到sa的imagepullsecret
            if (!CollectionUtils.isEmpty(nsImagePullSecrets)) {
                serviceAccountService.bindImagePullSecret(clusterId, namespace, serviceAccount, nsImagePullSecrets);
            }
        }
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
        namespace.setCreateTime(DateUtils.parseUTCDate(ns.getMetadata().getCreationTimestamp()));
        // 状态
        namespace.setPhase(ns.getStatus().getPhase());
        // 如果没有中文名称，则设置英文名称为中文名称
        if (StringUtils.isBlank(namespace.getAliasName())) {
            namespace.setAliasName(namespace.getName());
        }
        // 设置ns uid
        this.setContainerIdentityRange(ns, namespace);
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
                ns.setQuotas(rqMap.get(ns.getName()).getResourceQuotaDo());
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

    /**
     * 设置分区uid、gid范围
     * @param namespace
     * @param annotations
     */
    private void putContainerIdentityRange(Namespace namespace, Map<String, String> annotations) {
        JSONObject containerIdentityRange = new JSONObject();
        // 添加uid范围
        ContainerIdentityRange containerUIDRange = namespace.getContainerUIDRange();
        if (containerUIDRange != null) {
            containerIdentityRange.put(NamespaceConstant.KEY_CONTAINER_UID_RANGE, containerUIDRange);
        }
        // 添加gid范围
        ContainerIdentityRange containerGIDRange = namespace.getContainerGIDRange();
        if (containerGIDRange != null) {
            containerIdentityRange.put(NamespaceConstant.KEY_CONTAINER_GID_RANGE, containerGIDRange);
        }
        if (containerIdentityRange.keySet().size() != 0) {
            annotations.put(NamespaceConstant.KEY_CONTAINER_IDENTITY_RANGE, containerIdentityRange.toString());
        }
    }

    private void setContainerIdentityRange(io.fabric8.kubernetes.api.model.Namespace ns, Namespace namespace) {
        if (ns.getMetadata() != null && ns.getMetadata().getAnnotations() != null) {
            String containerIdentityRange = ns.getMetadata().getAnnotations().get(NamespaceConstant.KEY_CONTAINER_IDENTITY_RANGE);
            JSONObject rangeObj = JSONObject.parseObject(containerIdentityRange);
            if (rangeObj != null) {
                String uidRange = rangeObj.getString(NamespaceConstant.KEY_CONTAINER_UID_RANGE);
                if (StringUtils.isNotEmpty(uidRange)) {
                    namespace.setContainerUIDRange(JSONObject.parseObject(uidRange, ContainerIdentityRange.class));
                }
                String gidRange = rangeObj.getString(NamespaceConstant.KEY_CONTAINER_GID_RANGE);
                if (StringUtils.isNotEmpty(gidRange)) {
                    namespace.setContainerGIDRange(JSONObject.parseObject(gidRange, ContainerIdentityRange.class));
                }
            }
        }
    }

    /**
     * 更新数据库中的分区中文名
     * @param clusterId
     * @param namespace
     * @param aliasName
     */
    private void updateAliasName(String clusterId, String namespace, String aliasName) {
        QueryWrapper<BeanProjectNamespace> wrapper = new QueryWrapper<>();
        wrapper.eq("namespace", namespace);
        wrapper.eq("cluster_id", clusterId);
        List<BeanProjectNamespace> namespaces = beanProjectNamespaceMapper.selectList(wrapper);
        for (BeanProjectNamespace beanProjectNamespace : namespaces) {
            beanProjectNamespace.setAliasName(aliasName);
            beanProjectNamespaceMapper.updateById(beanProjectNamespace);
        }
    }

    public boolean checkAliasNameExist(String clusterId, String aliasName) {
        List<io.fabric8.kubernetes.api.model.Namespace> nsList = namespaceWrapper.list(clusterId);
        for (io.fabric8.kubernetes.api.model.Namespace ns : nsList) {
            if (ns.getMetadata().getName().equalsIgnoreCase(aliasName)) {
                return true;
            }
            if (ns.getMetadata().getAnnotations() != null && ns.getMetadata().getAnnotations().containsKey("alias_name")
                    && aliasName.equals(ns.getMetadata().getAnnotations().get("alias_name"))) {
                return true;
            }
        }
        return false;
    }

    public boolean onlyCheckAliasNameExist(String clusterId, String aliasName) {
        List<io.fabric8.kubernetes.api.model.Namespace> nsList = namespaceWrapper.list(clusterId);
        for (io.fabric8.kubernetes.api.model.Namespace ns : nsList) {
            if (ns.getMetadata().getAnnotations() != null && ns.getMetadata().getAnnotations().containsKey("alias_name")
                    && aliasName.equals(ns.getMetadata().getAnnotations().get("alias_name"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 修改分区注册状态
     */
    public void register(String clusterId, String name, Boolean registered, io.fabric8.kubernetes.api.model.Namespace ns){
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

}
