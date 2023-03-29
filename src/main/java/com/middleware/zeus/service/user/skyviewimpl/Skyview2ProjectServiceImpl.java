package com.middleware.zeus.service.user.skyviewimpl;

import java.util.*;
import java.util.stream.Collectors;

import com.middleware.caas.common.model.user.*;
import com.middleware.zeus.service.user.OrganizationService;
import com.middleware.zeus.skyview.v2.service.V2OrganService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.BackupServerDTO;
import com.middleware.caas.common.model.ResourceQuotaDo;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.Namespace;
import com.middleware.zeus.annotation.Skyview;
import com.middleware.zeus.bean.user.BeanProject;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.service.user.abstractService.AbstractProjectService;
import com.middleware.zeus.skyview.v2.service.V2ProjectService;
import com.middleware.zeus.util.ZeusCurrentUser;

import lombok.extern.slf4j.Slf4j;

/**
 * @author liyinlong
 * @since 2022/6/14 5:28 下午
 */
@Slf4j
@Service
@Skyview(target = "skyview2")
public class Skyview2ProjectServiceImpl extends AbstractProjectService implements ProjectService {

    public static final Set<Namespace> ALL_PROJECT_NS_LIST = new HashSet<>();

    @Autowired
    private V2ProjectService v2ProjectService;
    @Autowired
    private V2OrganService v2OrganService;

    @Override
    public void add(ProjectDto projectDto) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void add(BeanProject beanProject) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public List<ProjectDto> list(String organId) {
        return v2ProjectService.list(organId);
    }

    @Override
    public List<ProjectDto> list(String organId, String keyword) {
        List<ProjectDto> projectDtoList = this.list(organId);
        // 根据key进行过滤
        if (StringUtils.isNotEmpty(keyword)) {
            projectDtoList =
                projectDtoList.stream().filter(projectDto -> StringUtils.isNotEmpty(projectDto.getAliasName())
                    && projectDto.getAliasName().contains(keyword)).collect(Collectors.toList());
        }
        // 权限过滤
        if (!ZeusCurrentUser.isAdmin()) {
            List<ProjectDto> currentProjectList = v2ProjectService.switchTenants(organId);
            projectDtoList = projectDtoList.stream()
                .filter(projectDto -> currentProjectList.stream()
                    .anyMatch(currentProject -> projectDto.getProjectId().equals(currentProject.getProjectId())))
                .collect(Collectors.toList());
        }
        return projectDtoList;
    }

    @Override
    public List<Namespace> getNamespace(String organId, String projectId, String clusterId, Boolean withQuota,
        Boolean withMiddleware) {
        List<Namespace> nsList = v2ProjectService.nsList(organId, projectId, withQuota);
        // 根据集群id过滤
        if(StringUtils.isNotEmpty(clusterId)){
            nsList = nsList.stream().filter(ns -> ns.getClusterId().equals(clusterId)).collect(Collectors.toList());
        }
        if (withMiddleware) {
            Map<String, List<Namespace>> namespaceMap =
                nsList.stream().collect(Collectors.groupingBy(Namespace::getClusterId));
            for (String key : namespaceMap.keySet()) {
                namespaceService.listNamespaceWithMiddleware(namespaceMap.get(key), key);
            }
        }
        return nsList;
    }

    @Override
    public List<Namespace> getNamespace(String organId, String projectId) {
        return getNamespace(organId, projectId, null, false, false);
    }

    @Override
    public List<ProjectNamespaceDo> listNamespace(String clusterId) {
        if (CollectionUtils.isEmpty(ALL_PROJECT_NS_LIST)){
            // todo  考虑在前端进行数据的分别查询
            refreshProjectNamespace();
            //
        }
        List<Namespace> allProjectNsList = new ArrayList<>(ALL_PROJECT_NS_LIST);

        if (StringUtils.isNotEmpty(clusterId)){
            allProjectNsList = allProjectNsList.stream().filter(ns -> ns.getClusterId().equals(clusterId)).collect(Collectors.toList());
        }
        return allProjectNsList.stream().map(ns -> {
            ProjectNamespaceDo projectNamespaceDo = new ProjectNamespaceDo();
            BeanUtils.copyProperties(ns, projectNamespaceDo);
            projectNamespaceDo.setNamespace(ns.getName());
            return projectNamespaceDo;
        }).collect(Collectors.toList());
    }

    @Override
    @Deprecated
    public List<MiddlewareClusterDTO> getAllocatableNamespace() {
        return null;
    }

    @Override
    public List<UserDto> getUser(String organId, String projectId, Boolean allocatable) {
        ProjectDto projectDto = v2ProjectService.get(organId, projectId, false, false);
        return projectDto.getUserDtoList();
    }

    @Override
    public void bindUser(ProjectDto projectDto) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void updateUserRole(String organId, String projectId, UserDto userDto) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void unbindUser(String organId, String projectId, String username) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void delete(String organId, String projectId) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void update(ProjectDto projectDto) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void update(BeanProject beanProject) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void addNamespace(Namespace namespace) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void bindNamespace(Namespace namespace) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void bindNamespace(List<Namespace> namespaceList) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void unBindNamespace(String organId, String projectId, String clusterId, String namespace,
        Boolean checkExist) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void unBindNamespace(String organId, String projectId, String clusterId, String namespace) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public Set<String> getRelationClusterIds(String organId, String projectId) {
        List<Namespace> namespaceList = v2ProjectService.nsList(organId, projectId, false);
        return namespaceList.stream().map(Namespace::getClusterId).collect(Collectors.toSet());
    }

    @Override
    public ProjectDto get(String organId, String projectId) {
        return v2ProjectService.get(organId, projectId, false, false);
    }

    @Override
    public void allocateQuota(ProjectQuota projectQuota) {
        allocateBackupServer(projectQuota);
    }

    @Override
    public List<ResourceQuotaDo> getStorageQuota(String organId, String projectId, String clusterId, boolean detail) {
        List<Namespace> nsList = v2ProjectService.nsList(organId, projectId, true);
        List<ResourceQuotaDo> quotaDoList = nsList.stream().map(Namespace::getQuotas).collect(Collectors.toList());

        // 将分区配额以集群维度聚合
        Map<String, List<ResourceQuotaDo>> rqListMap =
            quotaDoList.stream().collect(Collectors.groupingBy(ResourceQuotaDo::getClusterId));
        List<ResourceQuotaDo> resourceQuotaDoList = new ArrayList<>();
        for (String key : rqListMap.keySet()) {
            ResourceQuotaDo resourceQuotaDo = resourceQuotaService.calculateQuota(rqListMap.get(key));
            resourceQuotaDoList.add(resourceQuotaDo);
        }
        return resourceQuotaDoList;
    }

    @Override
    public void removeStorageQuota(String organId, String projectId, String storageId, String clusterId) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public List<ResourceQuotaDo> getCpuMemoryQuota(String organId, String projectId, boolean detail) {
        // todo
        List<Namespace> nsList = v2ProjectService.nsList(organId, projectId, true);
        List<ResourceQuotaDo> quotaDoList = nsList.stream().map(Namespace::getQuotas).collect(Collectors.toList());

        // 将分区配额以集群维度聚合
        Map<String, List<ResourceQuotaDo>> rqListMap =
            quotaDoList.stream().collect(Collectors.groupingBy(ResourceQuotaDo::getClusterId));
        List<ResourceQuotaDo> resourceQuotaDoList = new ArrayList<>();
        for (String key : rqListMap.keySet()) {
            ResourceQuotaDo resourceQuotaDo = resourceQuotaService.calculateQuota(rqListMap.get(key));
            resourceQuotaDoList.add(resourceQuotaDo);
        }
        return resourceQuotaDoList;
    }

    @Override
    public void removeCpuMemoryQuota(String organId, String projectId, String clusterId) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    public void refreshProjectNamespace(){
        List<Namespace> allNsList = new ArrayList<>();
        List<OrganizationDto> organList = v2OrganService.list();
        if (!CollectionUtils.isEmpty(organList)){
            for (OrganizationDto organizationDto : organList){
                List<Namespace> nsList = v2OrganService.nsList(organizationDto.getOrganId());
                allNsList.addAll(nsList);
            }
        }
        ALL_PROJECT_NS_LIST.addAll(allNsList);
    }

}
