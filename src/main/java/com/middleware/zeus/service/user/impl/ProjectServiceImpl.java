package com.middleware.zeus.service.user.impl;

import static com.middleware.caas.common.constants.CommonConstant.NUM_TWO;
import static com.middleware.caas.common.constants.NameConstant.*;
import static com.middleware.caas.common.constants.user.UserConstant.USERNAME;

import java.util.*;
import java.util.stream.Collectors;

import com.middleware.zeus.bean.user.BeanRole;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.enums.DictEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.*;
import com.middleware.caas.common.model.middleware.ImageRepositoryDTO;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.Namespace;
import com.middleware.caas.common.model.middleware.StorageClassInfo;
import com.middleware.caas.common.model.user.*;
import com.middleware.caas.filters.token.JwtTokenComponent;
import com.middleware.caas.filters.user.CurrentUser;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.tool.uuid.UUIDUtils;
import com.middleware.zeus.annotation.Skyview;
import com.middleware.zeus.bean.user.BeanProject;
import com.middleware.zeus.bean.user.BeanProjectNamespace;
import com.middleware.zeus.dao.user.BeanProjectMapper;
import com.middleware.zeus.dao.user.BeanProjectNamespaceMapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.service.k8s.MiddlewareCRService;
import com.middleware.zeus.service.k8s.NamespaceService;
import com.middleware.zeus.service.k8s.ServiceAccountService;
import com.middleware.zeus.service.k8s.StorageService;
import com.middleware.zeus.service.middleware.BackupPositionService;
import com.middleware.zeus.service.middleware.ImageRepositoryService;
import com.middleware.zeus.service.middleware.MiddlewareInfoService;
import com.middleware.zeus.service.user.*;
import com.middleware.zeus.service.user.abstractService.AbstractProjectService;
import com.middleware.zeus.util.AssertUtil;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2022/3/24 9:25 上午
 */
@Service
@Slf4j
@Skyview(target = "zeus")
public class ProjectServiceImpl extends AbstractProjectService implements ProjectService {

    @Autowired
    private BeanProjectMapper beanProjectMapper;
    @Autowired
    public BeanProjectNamespaceMapper beanProjectNamespaceMapper;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    public UserService userService;
    @Autowired
    public MiddlewareCRService middlewareCRService;
    @Autowired
    public MiddlewareInfoService middlewareInfoService;
    @Autowired
    private ServiceAccountService serviceAccountService;
    @Autowired
    private ImageRepositoryService imageRepositoryService;
    @Value("${system.privateRegistry.middlewareServiceAccount:default}")
    private String middlewareServiceAccount;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private BackupPositionService backupPositionService;
    @Autowired
    private PlatformQuotaService platformQuotaService;
    @Autowired
    private StorageService storageService;
    @Autowired
    private OrganizationService organizationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(ProjectDto projectDto) {
        AssertUtil.notBlank(projectDto.getName(), DictEnum.PROJECT_NAME);
        projectDto.setAliasName(projectDto.getName());
        checkParam(projectDto);
        String projectId = UUIDUtils.get16UUID();
        BeanProject beanProject = new BeanProject();
        BeanUtils.copyProperties(projectDto, beanProject);
        beanProject.setProjectId(projectId);
        beanProject.setCreateTime(new Date());
        // 添加项目
        beanProjectMapper.insert(beanProject);
        // 绑定用户角色
        if (!CollectionUtils.isEmpty(projectDto.getUserDtoList())) {
            for (UserDto userDto : projectDto.getUserDtoList()){
                userRoleService.insert(projectDto.getOrganId(), projectId, userDto.getUserName(), 2);
            }
        }
    }

    @Override
    public List<ProjectDto> list(String organId) {
        QueryWrapper<BeanProject> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(organId)){
            wrapper.eq("organ_id", organId);
        }
        List<BeanProject> beanProjectList = beanProjectMapper.selectList(wrapper);
        return beanProjectList.stream().map(bean -> {
            ProjectDto projectDto = new ProjectDto();
            BeanUtils.copyProperties(bean, projectDto);
            return projectDto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ProjectDto> list(String organId, String keyword) {
        // 查询项目列表
        List<ProjectDto> list = this.list(organId);
        //获取当前用户
        CurrentUser currentUser = CurrentUserRepository.getUserExistNull();
        JSONObject user = JwtTokenComponent.checkToken(currentUser.getToken()).getValue();
        // 获取当前用户所在所有项目内的角色信息
        UserDto userDto = userService.getUserDto(user.getString(USERNAME));
        Map<String, UserRole> userRoleMap =
                userDto.getUserRoleList().stream().filter(userRole -> userRole.getProjectId()!=null).collect(Collectors.toMap(UserRole::getProjectId, u -> u));
        // 获取组织管理员信息
        BeanRole role = roleService.getOrganManagerRoleId();
        // 判断是否为admin,并进行过滤
        if (!userDto.getIsAdmin()) {
            list = list.stream()
                .filter(projectDto -> userDto.getUserRoleList().stream()
                    .anyMatch(userRole -> StringUtils.isNotEmpty(userRole.getOrganId())
                        && userRole.getOrganId().equals(organId)
                        && ((userRole.getRoleId() != null
                            && userRole.getRoleId().equals(role.getId()))
                            || (StringUtils.isNotEmpty(userRole.getProjectId())
                                && userRole.getProjectId().equals(projectDto.getProjectId())))))
                .collect(Collectors.toList());
        }
        // 获取项目下所有分区
        QueryWrapper<BeanProjectNamespace> nsWrapper = new QueryWrapper<>();
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(nsWrapper);
        Map<String, List<BeanProjectNamespace>> beanProjectNamespaceListMap =
                beanProjectNamespaceList.stream().collect(Collectors.groupingBy(BeanProjectNamespace::getProjectId));

        // 获取项目用户列表
        List<UserRole> userRoleList = userRoleService.list().stream()
            .filter(userRole -> StringUtils.isNotEmpty(userRole.getProjectId())).collect(Collectors.toList());
        Map<String, List<UserRole>> userRoleListMap =
            userRoleList.stream().collect(Collectors.groupingBy(UserRole::getProjectId));

        // 获取项目可用备份服务器
        List<ProjectBackupServerDTO> projectBackupServerDTOList =
            projectBackupServerService.listByProjectId(organId, null);
        Map<String, List<ProjectBackupServerDTO>> projectBackupServerIdMap =
            projectBackupServerDTOList.stream().collect(Collectors.groupingBy(ProjectBackupServerDTO::getProjectId));

        // 封装数据
        List<ProjectDto> projectDtoList = new ArrayList<>();
        for (ProjectDto projectDto : list) {
            projectDto
                .setMemberCount(userRoleListMap.getOrDefault(projectDto.getProjectId(), new ArrayList<>()).size());
            if (beanProjectNamespaceListMap.containsKey(projectDto.getProjectId())) {
                projectDto.setNamespaceCount(beanProjectNamespaceListMap.get(projectDto.getProjectId()).size());
            }
            if (userDto.getIsAdmin()) {
                projectDto.setRoleId(1);
                projectDto.setRoleName("超级管理员");
            } else {
                if (userRoleMap.containsKey(projectDto.getProjectId())){
                    projectDto.setRoleId(userRoleMap.get(projectDto.getProjectId()).getRoleId());
                    projectDto.setRoleName(userRoleMap.get(projectDto.getProjectId()).getRoleName());
                    projectDto.setRoleWeight(userRoleMap.get(projectDto.getProjectId()).getWeight());
                }else {
                    projectDto.setRoleId(role.getId());
                    projectDto.setRoleName(role.getName());
                    projectDto.setRoleWeight(role.getWeight());
                }
            }
            // 设置备份服务器
            if (projectBackupServerIdMap.containsKey(projectDto.getProjectId())) {
                projectDto.setBackupServerList(projectBackupServerIdMap.get(projectDto.getProjectId()).stream()
                    .map(ProjectBackupServerDTO::getBackupServerId).collect(Collectors.toList()));
            }

            projectDtoList.add(projectDto);
        }

        // 根据key进行过滤
        if (StringUtils.isNotEmpty(keyword)) {
            projectDtoList = projectDtoList.stream()
                .filter(projectDto -> (StringUtils.isNotEmpty(projectDto.getAliasName())
                    && projectDto.getAliasName().contains(keyword))
                    || (StringUtils.isNotEmpty(projectDto.getDescription())
                        && projectDto.getDescription().contains(keyword)))
                .collect(Collectors.toList());
        }
        return projectDtoList;
    }

    @Override
    public List<MiddlewareClusterDTO> getAllocatableNamespace() {
        List<MiddlewareClusterDTO> clusterList = clusterService.listClusters(true, null);
        QueryWrapper<BeanProjectNamespace> wrapper = new QueryWrapper<>();
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);
        beanProjectNamespaceList.forEach(ns -> {
            if (StringUtils.isEmpty(ns.getAliasName())) {
                ns.setAliasName(ns.getNamespace());
            }
        });
        Map<String, List<BeanProjectNamespace>> nsMap =
            beanProjectNamespaceList.stream().collect(Collectors.groupingBy(BeanProjectNamespace::getClusterId));
        clusterList.forEach(cluster -> {
            List<Namespace> list = cluster.getNamespaceList().stream()
                .filter(ns -> !nsMap.containsKey(ns.getClusterId())
                    || nsMap.get(cluster.getId()).stream().noneMatch(pNs -> pNs.getNamespace().equals(ns.getName())))
                .collect(Collectors.toList());
            cluster.setNamespaceList(list);
        });
        return clusterList;
    }

    @Override
    public List<UserDto> getUser(String organId, String projectId, Boolean allocatable) {
        checkExist(organId, projectId);
        // 修改判断该用户是否可分配的逻辑
        List<UserDto> userDtoList = organizationService.listOrganUser(organId, false);
        if (allocatable) {
            // 获取可分配的
            userDtoList = userDtoList.stream()
                .filter(
                    userDto -> CollectionUtils.isEmpty(userDto.getUserRoleList()) || userDto.getUserRoleList().stream()
                        .noneMatch(userRole -> StringUtils.isNotEmpty(userRole.getProjectId())
                            && userRole.getProjectId().equals(projectId))
                        && userDto.getUserRoleList().stream().noneMatch(userRole -> userRole.getRoleId() == 1))
                .collect(Collectors.toList());
        } else {
            // 获取已分配的
            userDtoList = userDtoList.stream()
                .filter(userDto -> !CollectionUtils.isEmpty(userDto.getUserRoleList()) && userDto.getUserRoleList()
                    .stream().anyMatch(userRole -> StringUtils.isNotEmpty(userRole.getProjectId())
                        && userRole.getProjectId().equals(projectId)))
                .collect(Collectors.toList());
        }
        return userDtoList.stream().peek(userDto -> {
            if (!CollectionUtils.isEmpty(userDto.getUserRoleList())) {
                List<UserRole> userRoleList = userDto.getUserRoleList().stream()
                    .filter(userRole -> StringUtils.isNotEmpty(userRole.getProjectId())
                        && userRole.getProjectId().equals(projectId))
                    .collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(userRoleList)) {
                    userDto.setRoleId(userRoleList.get(0).getRoleId()).setRoleName(userRoleList.get(0).getRoleName());
                }
                userDto.setUserRoleList(null);
            }
        }).collect(Collectors.toList());
    }

    @Override
    public void bindUser(ProjectDto projectDto) {
        checkExist(projectDto.getOrganId(), projectDto.getProjectId());
        if (CollectionUtils.isEmpty(projectDto.getUserDtoList())){
            throw new BusinessException(ErrorMessage.PROJECT_ADD_USER_EMPTY_LIST);
        }
        projectDto.getUserDtoList().forEach(
            userDto -> userRoleService.insert(projectDto.getOrganId(), projectDto.getProjectId(), userDto.getUserName(), userDto.getRoleId()));
    }

    @Override
    public void updateUserRole(String organId, String projectId, UserDto userDto) {
        userRoleService.update(new UserRole().setOrganId(organId).setProjectId(projectId)
            .setUserName(userDto.getUserName()).setRoleId(userDto.getRoleId()));
    }

    @Override
    public void unbindUser(String organId, String projectId, String username) {
        userRoleService.delete(username, organId, projectId, null);
    }

    @Override
    public void delete(String organId, String projectId) {
        // 有服务存在不允许删除
        List<ProjectDto> projectDtoList = getMiddlewareCount(organId, projectId);
        if (!CollectionUtils.isEmpty(projectDtoList) && projectDtoList.get(0).getMiddlewareCount() != 0) {
            throw new BusinessException(ErrorMessage.PROJECT_IS_NOT_EMPTY);
        }
        // 删除项目
        QueryWrapper<BeanProject> wrapper =
            new QueryWrapper<BeanProject>().eq("organ_id", organId).eq("project_id", projectId);
        beanProjectMapper.delete(wrapper);
        // 解绑项目下分区
        unBindNamespace(organId, projectId, null, null);
        // 解绑项目下用户
        unbindUser(organId, projectId, null);
        // 回收资源
        platformQuotaService.remove(PROJECT, projectId, null, null, CPU, MEMORY, STORAGE);
        // 解绑项目下备份位置
        unBindBackupPosition(organId, projectId);
        // 解绑项目下备份服务器
        unBindBackupServer(organId, projectId);
    }

    @Override
    public void update(ProjectDto projectDto) {
        BeanProject beanProject = checkExist(projectDto.getOrganId(), projectDto.getProjectId());
        beanProject.setName(projectDto.getName());
        beanProject.setAliasName(projectDto.getName());
        beanProject.setDescription(projectDto.getDescription());
        beanProjectMapper.updateById(beanProject);
        // 绑定用户角色
        if (!CollectionUtils.isEmpty(projectDto.getUserDtoList())) {
            userRoleService.delete(null, projectDto.getOrganId(), projectDto.getProjectId(), 2);
            for (UserDto userDto : projectDto.getUserDtoList()){
                userRoleService.insert(projectDto.getOrganId(), projectDto.getProjectId(), userDto.getUserName(), 2);
            }
        }
    }

    @Override
    public void update(BeanProject beanProject) {
        beanProjectMapper.updateById(beanProject);
    }

    @Override
    public void addNamespace(Namespace namespace) {
        // 创建分区
        Map<String, String> labels = new HashMap<>();
        labels.put("middleware", "middleware");
        namespaceService.save(namespace, labels, true);
        // 绑定项目
        bindNamespace(namespace);
    }

    @Override
    public void unBindNamespace(String organId, String projectId, String clusterId, String namespace, Boolean checkExist) {
        if (checkExist) {
            List<MiddlewareCR> middlewareCRList = middlewareCRService.listCR(clusterId, namespace, null);
            if (!CollectionUtils.isEmpty(middlewareCRList)) {
                throw new BusinessException(ErrorMessage.NAMESPACE_IS_NOT_EMPTY);
            }
        }
        this.unBindNamespace(organId, projectId, clusterId, namespace);
    }

    @Override
    public void unBindNamespace(String organId, String projectId, String clusterId, String namespace) {
        QueryWrapper<BeanProjectNamespace> wrapper = new QueryWrapper<BeanProjectNamespace>();
        if (StringUtils.isNotEmpty(organId)) {
            wrapper.eq("organ_id", organId);
        }
        if (StringUtils.isNotEmpty(projectId)) {
            wrapper.eq("project_id", projectId);
        }
        if (StringUtils.isNotEmpty(clusterId)) {
            wrapper.eq("cluster_id", clusterId);
        }
        if (StringUtils.isNotEmpty(namespace)) {
            wrapper.eq("namespace", namespace);
        }
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(beanProjectNamespaceList)) {
            beanProjectNamespaceMapper.delete(wrapper);
        }
    }

    @Override
    public ProjectDto get(String organId, String projectId) {
        QueryWrapper<BeanProject> wrapper = new QueryWrapper<BeanProject>().eq("project_id", projectId);
        List<BeanProject> beanProjectList = beanProjectMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(beanProjectList)) {
            return null;
        }
        ProjectDto projectDto = new ProjectDto();
        BeanUtils.copyProperties(beanProjectList.get(0), projectDto);
        // 设置用户信息
        List<UserDto> userDtoList = getUser(organId, projectId, false);
        if (!CollectionUtils.isEmpty(userDtoList)) {
            userDtoList = userDtoList.stream()
                .filter(userDto -> userDto.getRoleId() != null && userDto.getRoleId().equals(NUM_TWO))
                .collect(Collectors.toList());
        }
        projectDto.setUserDtoList(userDtoList);
        return projectDto;
    }

    @Override
    public void allocateQuota(ProjectQuota projectQuota) {
        // 处理cpu\memory\storage
        if (!CollectionUtils.isEmpty(projectQuota.getQuotaList())) {
            // todo
            checkResource(projectQuota.getQuotaList());
            platformQuotaService.remove(PROJECT, projectQuota.getProjectId(), null, null, CPU, MEMORY, STORAGE);
            for (ResourceQuotaDo resourceQuotaDo : projectQuota.getQuotaList()){
                platformQuotaService.allocate(PROJECT, projectQuota.getProjectId(), resourceQuotaDo);
            }
        }
        // 记录备份服务器
        allocateBackupServer(projectQuota);
    }

    @Override
    public List<ResourceQuotaDo> getStorageQuota(String organId, String projectId, String clusterId, boolean detail) {
        // 获取租户自身存储配额
        List<ResourceQuotaDo> resourceQuotaDoList = platformQuotaService.getQuota(PROJECT, projectId, clusterId, STORAGE);
        // 设置存储名称
        platformQuotaService.convertStorageName(resourceQuotaDoList);
        if (detail) {
            List<ResourceQuotaDo> namespaceQuotaList = new ArrayList<>();
            // 获取指定组织 项目下的所有分区
            List<Namespace> namespaceList = getNamespace(organId, projectId, null, true, false);
            for (ResourceQuotaDo resourceQuotaDo : resourceQuotaDoList) {
                // 过滤获取指定集群下的分区列表
                List<Namespace> nsListFilterByClusterId =
                    namespaceList.stream().filter(ns -> ns.getClusterId().equals(resourceQuotaDo.getClusterId()))
                        .collect(Collectors.toList());
                // 分区存储资源设置存储id
                List<StorageClassInfo> storageClassInfoList =
                    storageService.listStorageClassInfo(resourceQuotaDo.getClusterId(), false);
                Map<String, String> storageClassIdMap =
                    storageClassInfoList.stream().filter(sc -> StringUtils.isNotEmpty(sc.getStorageId()))
                        .collect(Collectors.toMap(StorageClassInfo::getName, StorageClassInfo::getStorageId));
                List<ResourceQuotaDo> nsResourceQuotaList =
                    nsListFilterByClusterId.stream().filter(ns -> ns.getQuotas() != null).map(ns -> {
                        ResourceQuotaDo nsQuotas = ns.getQuotas();
                        for (StorageQuota storageQuota : nsQuotas.getStorageList()) {
                            if (storageClassIdMap.containsKey(storageQuota.getName())) {
                                storageQuota.setStorageId(storageClassIdMap.get(storageQuota.getName()));
                            }
                        }
                        return nsQuotas;
                    }).collect(Collectors.toList());
                // 计算多分区配额总和
                ResourceQuotaDo namespaceQuota = resourceQuotaService.calculateQuota(nsResourceQuotaList);
                namespaceQuota.setClusterId(resourceQuotaDo.getClusterId());
                namespaceQuotaList.add(namespaceQuota);
            }
            platformQuotaService.convertUsedResource(resourceQuotaDoList, namespaceQuotaList);
        }
        // 设置集群名称
        Map<String, String> clusterNickNameMap = clusterService.getClusterAliasName();
        for (ResourceQuotaDo resourceQuotaDo : resourceQuotaDoList) {
            if (StringUtils.isNotEmpty(resourceQuotaDo.getClusterId())
                && clusterNickNameMap.containsKey(resourceQuotaDo.getClusterId())) {
                resourceQuotaDo.setClusterNickName(clusterNickNameMap.get(resourceQuotaDo.getClusterId()));
            }
        }
        return resourceQuotaDoList;
    }

    @Override
    public void removeStorageQuota(String organId, String projectId, String storageId, String clusterId) {
        StorageDto storageDto = storageService.getById(clusterId, storageId);
        List<String> storageNameList =
            storageDto.getStorageClassList().stream().map(StorageClassInfo::getName).collect(Collectors.toList());
        List<Namespace> namespaceList = getNamespace(organId, projectId, clusterId, true, false);
        boolean exist = namespaceList.stream().anyMatch(namespace -> {
            boolean flag = false;
            ResourceQuotaDo quotaDo = namespace.getQuotas();
            if (quotaDo != null) {
                for (StorageQuota storageQuota : quotaDo.getStorageList()) {
                    flag = storageNameList.stream().anyMatch(scName -> scName.equals(storageQuota.getName()));
                }
            }
            return flag;
        });
        if (exist) {
            throw new BusinessException(ErrorMessage.PROJECT_STORAGE_USING);
        }
        // 删除存储
        platformQuotaService.remove(PROJECT, projectId, null, storageId, STORAGE);
    }

    @Override
    public List<ResourceQuotaDo> getCpuMemoryQuota(String organId, String projectId, boolean detail) {
        // 获取租户自身cpu memory 配额
        List<ResourceQuotaDo> resourceQuotaDoList = platformQuotaService.getQuota(PROJECT, projectId, null, CPU, MEMORY);
        if (detail) {
            List<ResourceQuotaDo> namespaceQuotaList = new ArrayList<>();
            for (ResourceQuotaDo resourceQuotaDo : resourceQuotaDoList){
                List<Namespace> namespaceList = getNamespace(organId, projectId, resourceQuotaDo.getClusterId(), true, false);
                ResourceQuotaDo namespaceQuota = resourceQuotaService.calculateQuota(namespaceList.stream().map(Namespace::getQuotas).collect(Collectors.toList()));
                namespaceQuota.setClusterId(resourceQuotaDo.getClusterId());
                namespaceQuotaList.add(namespaceQuota);
            }
            platformQuotaService.convertUsedResource(resourceQuotaDoList, namespaceQuotaList);
        }
        // 设置集群别名
        Map<String, String> clusterNickNameMap = clusterService.getClusterAliasName();
        return resourceQuotaDoList.stream().peek(resourceQuotaDo -> {
            if (StringUtils.isNotEmpty(resourceQuotaDo.getClusterId())
                && clusterNickNameMap.containsKey(resourceQuotaDo.getClusterId())) {
                resourceQuotaDo.setClusterNickName(clusterNickNameMap.get(resourceQuotaDo.getClusterId()));
            }
        }).collect(Collectors.toList());
    }

    @Override
    public void removeCpuMemoryQuota(String organId, String projectId, String clusterId) {
        List<Namespace> namespaceList = getNamespace(organId, projectId, clusterId, true, false);
        boolean exist = namespaceList.stream().anyMatch(namespace -> {
            boolean flag = false;
            ResourceQuotaDo quotaDo = namespace.getQuotas();
            if (quotaDo != null) {
                if ((quotaDo.getCpu() != null && quotaDo.getCpu().getRequest() != null
                    && quotaDo.getCpu().getRequest() > 0)
                    || (quotaDo.getMemory() != null && quotaDo.getMemory().getRequest() != null
                        && quotaDo.getMemory().getRequest() > 0)) {
                    flag = true;
                }

            }
            return flag;
        });
        if (exist) {
            throw new BusinessException(ErrorMessage.PROJECT_CPU_MEMORY_USING);
        }
        // 删除cpu memory
        platformQuotaService.remove(PROJECT, projectId, null, null, CPU, MEMORY);
    }

    @Override
    public void bindNamespace(Namespace namespace) {
        QueryWrapper<BeanProjectNamespace> wrapper = new QueryWrapper<BeanProjectNamespace>()
            .eq("namespace", namespace.getName()).eq("cluster_id", namespace.getClusterId());
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(beanProjectNamespaceList)) {
            throw new BusinessException(ErrorMessage.PROJECT_NAMESPACE_ALREADY_BIND);
        }
        AssertUtil.notBlank(namespace.getOrganId(), DictEnum.PROJECT_ID);
        AssertUtil.notBlank(namespace.getProjectId(), DictEnum.ORGAN_ID);
        AssertUtil.notBlank(namespace.getName(), DictEnum.NAMESPACE_NAME);
        BeanProjectNamespace beanProjectNamespace = new BeanProjectNamespace();
        BeanUtils.copyProperties(namespace, beanProjectNamespace);
        beanProjectNamespace.setNamespace(namespace.getName());
        beanProjectNamespaceMapper.insert(beanProjectNamespace);
        // 给分区默认serviceAccount绑定imagePullSecret
        checkAndBindImagePullSecret(namespace.getClusterId(), namespace.getName());
    }

    @Override
    public void bindNamespace(List<Namespace> namespaceList) {
        namespaceList.forEach(namespace -> {
            try {
                bindNamespace(namespace);
            } catch (Exception e) {
                log.error("绑定分区出错了", e);
            }
        });
    }

    @Override
    public void add(BeanProject beanProject) {
        beanProjectMapper.insert(beanProject);
    }

    @Override
    public Set<String> getRelationClusterIds(String organId, String projectId) {
        QueryWrapper<BeanProjectNamespace> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(organId)){
            wrapper.eq("organ_id", organId);
        }
        wrapper.eq("project_id", projectId);
        List<BeanProjectNamespace> projectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);
        return projectNamespaceList.stream().map(BeanProjectNamespace::getClusterId).collect(Collectors.toSet());
    }

    @Override
    public List<Namespace> getNamespace(String organId, String projectId) {
        return getNamespace(organId, projectId, null, false, false);
    }

    @Override
    public List<ProjectNamespaceDo> listNamespace(String clusterId) {
        // 获取分区项目绑定关系
        QueryWrapper<BeanProjectNamespace> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(clusterId)){
            wrapper.eq("cluster_id", clusterId);
        }
        List<BeanProjectNamespace> projectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);

        // 获取项目信息
        QueryWrapper<BeanProject> pjWrapper = new QueryWrapper<>();
        List<BeanProject> beanProjectList = beanProjectMapper.selectList(pjWrapper);
        Map<String, String> projectNameMap = beanProjectList.stream().collect(Collectors.toMap(BeanProject::getProjectId, BeanProject::getName));

        List<ProjectNamespaceDo> projectNamespaceDoList = new ArrayList<>();
        for (BeanProjectNamespace beanProjectNamespace : projectNamespaceList){
            ProjectNamespaceDo projectNamespaceDo = new ProjectNamespaceDo();
            BeanUtils.copyProperties(beanProjectNamespace, projectNamespaceDo);
            projectNamespaceDo.setProjectName(projectNameMap.get(projectNamespaceDo.getProjectId()));

            projectNamespaceDoList.add(projectNamespaceDo);
        }
        return projectNamespaceDoList;
    }

    @Override
    public List<Namespace> getNamespace(String organId, String projectId, String clusterId, Boolean withQuota, Boolean withMiddleware) {
        QueryWrapper<BeanProjectNamespace> wrapper =
            new QueryWrapper<>();
        if (StringUtils.isNotEmpty(organId)){
            wrapper.eq("organ_id", organId);
        }
        if (StringUtils.isNotEmpty(projectId)){
            wrapper.eq("project_id", projectId);
        }
        if (!StringUtils.isEmpty(clusterId)) {
            wrapper.eq("cluster_id", clusterId);
        }
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);
        List<Namespace> namespaces = beanProjectNamespaceList.stream().map(beanProjectNamespace -> {
            Namespace namespace = new Namespace();
            BeanUtils.copyProperties(beanProjectNamespace, namespace);
            namespace.setClusterAliasName(clusterService.findById(namespace.getClusterId()).getNickname());
            namespace.setName(beanProjectNamespace.getNamespace());
            return namespace;
        }).collect(Collectors.toList());
        // 查询quota
        if (withQuota) {
            Map<String, List<Namespace>> namespaceMap =
                namespaces.stream().collect(Collectors.groupingBy(Namespace::getClusterId));
            for (String key : namespaceMap.keySet()) {
                namespaceService.listNamespaceWithQuota(namespaceMap.get(key), key);
            }
        }
        // with middleware
        if (withMiddleware){
            Map<String, List<Namespace>> namespaceMap =
                    namespaces.stream().collect(Collectors.groupingBy(Namespace::getClusterId));
            for (String key : namespaceMap.keySet()) {
                namespaceService.listNamespaceWithMiddleware(namespaceMap.get(key), key);
            }
        }

        if (StringUtils.isEmpty(clusterId)) {
            return namespaces;
        }
        return addOtherInfo(namespaces, clusterId);
    }

    /**
     * 删除项目关联的备份位置
     * @param projectId
     */
    private void unBindBackupPosition(String organId, String projectId){
        backupPositionService.deleteByProjectId(organId, projectId);
    }

    /**
     * 删除项目关联的备份服务器
     * @param organId 组织id
     * @param projectId 项目id
     */
    private void unBindBackupServer(String organId, String projectId){
        projectBackupServerService.deleteByProjectId(organId, projectId);
    }

    public void checkParam(ProjectDto projectDto){
        QueryWrapper<BeanProject> wrapper = new QueryWrapper<BeanProject>().eq("name", projectDto.getName());
        List<BeanProject> beanProjectList = beanProjectMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(beanProjectList)){
            throw new BusinessException(ErrorMessage.PROJECT_NAME_EXIST);
        }
    }

    /**
     * 设置分区别名
     * @param namespace
     */
    private void setNamespaceAliasName(Namespace namespace){
        Namespace ns = namespaceService.get(namespace.getClusterId(), namespace.getName());
        namespace.setAliasName(ns.getAliasName());
    }

    /**
     * 校验项目是否存在
     */
    public BeanProject checkExist(String organId, String projectId) {
        QueryWrapper<BeanProject> wrapper = new QueryWrapper<BeanProject>().eq("organ_id", organId).eq("project_id", projectId);
        List<BeanProject> beanProjectList = beanProjectMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(beanProjectList)) {
            throw new BusinessException(ErrorMessage.PROJECT_NOT_EXIST);
        }
        return beanProjectList.get(0);
    }

    /**
     * 设置分区其他信息
     *
     * @param namespaces
     * @param clusterId
     * @return
     */
    public List<Namespace> addOtherInfo(List<Namespace> namespaces, String clusterId) {
        try {
            List<Namespace> nsList = namespaceService.list(clusterId, false, null);
            Map<String, Namespace> nsMap = new HashMap<>();
            nsList.forEach(ns -> {
                nsMap.put(ns.getName(), ns);
            });
            namespaces.forEach(ns -> {
                Namespace namespace = nsMap.get(ns.getName());
                if (namespace != null) {
                    // 设置分区可用区状态
                    ns.setAvailableDomain(namespace.isAvailableDomain());
                    // 设置uid
                    ns.setContainerUIDRange(namespace.getContainerUIDRange());
                }
            });
            return namespaces;
        } catch (Exception e) {
            log.error("查询双活分区状态失败");
            return namespaces;
        }
    }

    private void checkAndBindImagePullSecret(String clusterId, String namespace) {
        ServiceAccount serviceAccount = serviceAccountService.get(clusterId, namespace, middlewareServiceAccount);
        if (serviceAccount == null) {
            return;
        }
        List<ImageRepositoryDTO> imageRepositoryDTOS = imageRepositoryService.list(clusterId);
        imageRepositoryService.createImagePullSecret(clusterId, namespace, imageRepositoryDTOS);
        List<Secret> allImagePullSecret = imageRepositoryService.listImagePullSecret(clusterId, namespace);
        serviceAccountService.bindImagePullSecret(clusterId, namespace, serviceAccount, allImagePullSecret);
    }

    public void checkResource(List<ResourceQuotaDo> resourceQuotaDoList){

    }

}
