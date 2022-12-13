package com.middleware.zeus.service.user.impl;

import static com.middleware.caas.common.constants.user.UserConstant.USERNAME;

import java.util.*;
import java.util.stream.Collectors;

import com.middleware.caas.common.enums.ComponentsEnum;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.service.middleware.ImageRepositoryService;
import com.middleware.caas.common.model.middleware.*;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.service.user.UserRoleService;
import com.middleware.zeus.service.user.UserService;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.enums.DictEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.user.ProjectDto;
import com.middleware.caas.common.model.user.UserDto;
import com.middleware.caas.common.model.user.UserRole;
import com.middleware.caas.filters.token.JwtTokenComponent;
import com.middleware.caas.filters.user.CurrentUser;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.tool.uuid.UUIDUtils;
import com.middleware.zeus.bean.BeanClusterMiddlewareInfo;
import com.middleware.zeus.bean.BeanMiddlewareInfo;
import com.middleware.zeus.bean.user.BeanProject;
import com.middleware.zeus.bean.user.BeanProjectNamespace;
import com.middleware.zeus.dao.user.BeanProjectMapper;
import com.middleware.zeus.dao.user.BeanProjectNamespaceMapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCR;
import com.middleware.zeus.service.middleware.ClusterMiddlewareInfoService;
import com.middleware.zeus.service.middleware.MiddlewareInfoService;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.util.AssertUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2022/3/24 9:25 上午
 */
@Service
@Slf4j
@ConditionalOnProperty(value = "system.usercenter", havingValue = "zeus")
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private BeanProjectMapper beanProjectMapper;
    @Autowired
    public BeanProjectNamespaceMapper beanProjectNamespaceMapper;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    public UserService userService;
    @Autowired
    public MiddlewareCRService middlewareCRService;
    @Autowired
    public MiddlewareInfoService middlewareInfoService;
    @Autowired
    private ClusterMiddlewareInfoService clusterMiddlewareInfoService;
    @Autowired
    private ClusterComponentService clusterComponentService;
    @Autowired
    private ServiceAccountService serviceAccountService;
    @Autowired
    private ImageRepositoryService imageRepositoryService;
    @Value("${system.privateRegistry.middlewareServiceAccount:default}")
    private String middlewareServiceAccount;
    @Autowired
    private NamespaceService namespaceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(ProjectDto projectDto) {
        AssertUtil.notBlank(projectDto.getName(), DictEnum.PROJECT_NAME);
        checkParam(projectDto);
        String projectId = UUIDUtils.get16UUID();
        BeanProject beanProject = new BeanProject();
        BeanUtils.copyProperties(projectDto, beanProject);
        beanProject.setProjectId(projectId);
        beanProject.setCreateTime(new Date());
        // 添加项目
        beanProjectMapper.insert(beanProject);
        // 绑定用户角色
        if (StringUtils.isNotEmpty(projectDto.getUser())) {
            userRoleService.insert(projectId, projectDto.getUser(), 2);
        }
        // 绑定分区
        if (projectDto.getClusterList() != null) {
            projectDto.getClusterList().forEach(cluster -> {
                if (cluster.getNamespaceList() != null) {
                    cluster.getNamespaceList().forEach(namespace -> this
                        .bindNamespace(namespace.setProjectId(projectId).setClusterId(cluster.getId())));
                }
            });
        }
    }

    @Override
    public List<ProjectDto> list(String keyword) {
        QueryWrapper<BeanProject> wrapper = new QueryWrapper<>();
        List<BeanProject> beanProjectList = beanProjectMapper.selectList(wrapper);
        CurrentUser currentUser = CurrentUserRepository.getUserExistNull();
        JSONObject user = JwtTokenComponent.checkToken(currentUser.getToken()).getValue();
        // 获取用户角色对应
        UserDto userDto = userService.list(null).stream().filter(u -> u.getUserName().equals(user.getString(USERNAME)))
            .collect(Collectors.toList()).get(0);
        Map<String, UserRole> userRoleMap =
            userDto.getUserRoleList().stream().collect(Collectors.toMap(UserRole::getProjectId, u -> u));
        // 是否为 admin
        boolean flag = userDto.getUserRoleList().stream().anyMatch(userRole -> userRole.getRoleId() == 1);
        // 非admin,进行过滤
        if (!flag) {
            beanProjectList = beanProjectList.stream()
                .filter(bp -> userDto.getUserRoleList().stream()
                    .anyMatch(userRole -> userRole.getProjectId().equals(bp.getProjectId())))
                .collect(Collectors.toList());
        }
        // 获取项目分区
        QueryWrapper<BeanProjectNamespace> nsWrapper = new QueryWrapper<>();
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(nsWrapper);
        Map<String, List<BeanProjectNamespace>> beanProjectNamespaceListMap =
            beanProjectNamespaceList.stream().collect(Collectors.groupingBy(BeanProjectNamespace::getProjectId));

        // 获取项目用户列表
        List<UserRole> userRoleList = userRoleService.list().stream()
            .filter(userRole -> StringUtils.isNotEmpty(userRole.getProjectId())).collect(Collectors.toList());
        Map<String, List<UserRole>> userRoleListMap =
            userRoleList.stream().collect(Collectors.groupingBy(UserRole::getProjectId));

        // 封装数据
        List<ProjectDto> projectDtoList = new ArrayList<>();
        for (BeanProject beanProject : beanProjectList) {
            ProjectDto projectDto = new ProjectDto();
            BeanUtils.copyProperties(beanProject, projectDto);
            projectDto
                .setMemberCount(userRoleListMap.getOrDefault(beanProject.getProjectId(), new ArrayList<>()).size());
            if (beanProjectNamespaceListMap.containsKey(projectDto.getProjectId())) {
                projectDto.setNamespaceCount(beanProjectNamespaceListMap.get(projectDto.getProjectId()).size());
            }
            if (flag) {
                projectDto.setRoleId(1);
                projectDto.setRoleName("超级管理员");
            } else {
                projectDto.setRoleId(userRoleMap.get(projectDto.getProjectId()).getRoleId());
                projectDto.setRoleName(userRoleMap.get(projectDto.getProjectId()).getRoleName());
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
    public List<UserDto> getUser(String projectId, Boolean allocatable) {
        checkExist(projectId);
        List<UserDto> userDtoList = userService.list(null);
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
        checkExist(projectDto.getProjectId());
        projectDto.getUserDtoList().forEach(
            userDto -> userRoleService.insert(projectDto.getProjectId(), userDto.getUserName(), userDto.getRoleId()));
    }

    @Override
    public void updateUserRole(String projectId, UserDto userDto) {
        userRoleService.update(userDto, projectId);
    }

    @Override
    public void unbindUser(String projectId, String username) {
        userRoleService.delete(username, projectId, null);
    }

    @Override
    public void delete(String projectId) {
        // 有服务存在不允许删除
        List<ProjectDto> projectDtoList = getMiddlewareCount(projectId);
        if (!CollectionUtils.isEmpty(projectDtoList) && projectDtoList.get(0).getMiddlewareCount() != 0) {
            throw new BusinessException(ErrorMessage.PROJECT_IS_NOT_EMPTY);
        }
        // 删除项目
        QueryWrapper<BeanProject> wrapper = new QueryWrapper<BeanProject>().eq("project_id", projectId);
        beanProjectMapper.delete(wrapper);
        // 解绑项目下分区
        unBindNamespace(projectId, null, null);
        // 解绑项目下用户
        unbindUser(projectId, null);
    }

    @Override
    public void update(ProjectDto projectDto) {
        BeanProject beanProject = checkExist(projectDto.getProjectId());
        beanProject.setAliasName(projectDto.getAliasName());
        beanProject.setDescription(projectDto.getDescription());
        beanProjectMapper.updateById(beanProject);
    }

    @Override
    public void update(BeanProject beanProject) {
        beanProjectMapper.updateById(beanProject);
    }

    @Override
    public void unBindNamespace(String projectId, String clusterId, String namespace, Boolean checkExist) {
        if (checkExist) {
            List<MiddlewareCR> middlewareCRList = middlewareCRService.listCR(clusterId, namespace, null);
            if (!CollectionUtils.isEmpty(middlewareCRList)) {
                throw new BusinessException(ErrorMessage.NAMESPACE_IS_NOT_EMPTY);
            }
        }
        this.unBindNamespace(projectId, clusterId, namespace);
    }

    @Override
    public void unBindNamespace(String projectId, String clusterId, String namespace) {
        QueryWrapper<BeanProjectNamespace> wrapper = new QueryWrapper<BeanProjectNamespace>();
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
    public List<ProjectMiddlewareResourceInfo> middlewareResource(String projectId) throws Exception {
        QueryWrapper<BeanProjectNamespace> wrapper =
            new QueryWrapper<BeanProjectNamespace>().eq("project_id", projectId);
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);
        // 获取集群
        Set<String> clusterIdSet = new HashSet<>();
        beanProjectNamespaceList.forEach(beanProjectNamespace -> {
            clusterIdSet.add(beanProjectNamespace.getClusterId());
        });
        // 获取集群下已安装中间件并集
        Set<String> mwTypeSet = new HashSet<>();
        for (String clusterId : clusterIdSet) {
            mwTypeSet.addAll(clusterMiddlewareInfoService.list(clusterId, true).stream()
                .map(BeanClusterMiddlewareInfo::getChartName).collect(Collectors.toList()));
        }
        // 查询用户角色项目权限
        String username =
            JwtTokenComponent.checkToken(CurrentUserRepository.getUser().getToken()).getValue().getString(USERNAME);
        UserDto userDto = userService.getUserDto(username);
        Map<String, String> power = new HashMap<>();
        if (!userDto.getIsAdmin()
            && userDto.getUserRoleList().stream().anyMatch(userRole -> userRole.getProjectId().equals(projectId))) {
            power
                .putAll(userDto.getUserRoleList().stream().filter(userRole -> userRole.getProjectId().equals(projectId))
                    .collect(Collectors.toList()).get(0).getPower());
        }
        // 过滤获取拥有权限的中间件
        if (!CollectionUtils.isEmpty(power)) {
            mwTypeSet = mwTypeSet.stream().filter(
                mwType -> power.keySet().stream().anyMatch(key -> !"0000".equals(power.get(key)) && mwType.equals(key)))
                .collect(Collectors.toSet());
        }
        // 查询数据
        List<MiddlewareResourceInfo> all = new ArrayList<>();
        for (String clusterId : clusterIdSet) {
            all.addAll(clusterService.getMwResource(clusterId));
        }
        // 根据分区过滤
        all = all.stream().filter(middlewareResourceInfo -> beanProjectNamespaceList.stream().anyMatch(
            beanProjectNamespace -> beanProjectNamespace.getNamespace().equals(middlewareResourceInfo.getNamespace())))
            .collect(Collectors.toList());
        // 获取image.path
        Map<String,
            String> middlewareImagePathMap = middlewareInfoService.list(false).stream()
                .filter(beanMiddlewareInfo -> beanMiddlewareInfo.getImagePath() != null)
                .collect(Collectors.toMap(BeanMiddlewareInfo::getChartName, BeanMiddlewareInfo::getImagePath));
        // 封装数据
        Map<String, List<MiddlewareResourceInfo>> map =
            all.stream().collect(Collectors.groupingBy(MiddlewareResourceInfo::getType));
        List<ProjectMiddlewareResourceInfo> infoList = new ArrayList<>();
        for (String mwType : mwTypeSet) {
            ProjectMiddlewareResourceInfo projectMiddlewareResourceInfo = new ProjectMiddlewareResourceInfo()
                .setType(mwType).setAliasName(MiddlewareOfficialNameEnum.findByChartName(mwType))
                .setMiddlewareResourceInfoList(map.getOrDefault(mwType, null))
                .setImagePath(middlewareImagePathMap.getOrDefault(mwType, null));
            infoList.add(projectMiddlewareResourceInfo);
        }
        infoList.sort(Comparator.comparing(ProjectMiddlewareResourceInfo::getType));
        return infoList;
    }

    @Override
    public List<ProjectDto> getMiddlewareCount(String projectId) {
        QueryWrapper<BeanProjectNamespace> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(projectId)) {
            wrapper.eq("project_id", projectId);
        }
        String username = CurrentUserRepository.getUser().getUsername();
        // 获取项目分区列表
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);
        // 查询用户信息
        UserDto userDto = userService.getUserDto(username);
        if (!userDto.getIsAdmin()) {
            QueryWrapper<BeanProject> projectQueryWrapper = new QueryWrapper<>();
            // 获取该用户所属的各个项目
            List<BeanProject> beanProjectList = beanProjectMapper.selectList(projectQueryWrapper).stream()
                .filter(beanProject -> userDto.getUserRoleList().stream()
                    .anyMatch(userRole -> userRole.getProjectId().equals(beanProject.getProjectId())))
                .collect(Collectors.toList());
            // 获取n个项目的分区
            if (!CollectionUtils.isEmpty(beanProjectList)) {
                beanProjectNamespaceList = beanProjectNamespaceList.stream()
                    .filter(beanProjectNamespace -> beanProjectList.stream().anyMatch(
                        beanProject -> beanProject.getProjectId().equals(beanProjectNamespace.getProjectId())))
                    .collect(Collectors.toList());
            }
        }
        // 分区为空
        if (CollectionUtils.isEmpty(beanProjectNamespaceList)) {
            return null;
        }
        Set<String> clusterIdList =
            beanProjectNamespaceList.stream().map(BeanProjectNamespace::getClusterId).collect(Collectors.toSet());
        Map<String, List<MiddlewareCR>> middlewareCRListMap = new HashMap<>();
        for (String clusterId : clusterIdList) {
            if (!clusterComponentService.checkInstalled(clusterId, ComponentsEnum.MIDDLEWARE_CONTROLLER.getName())) {
                continue;
            }
            List<MiddlewareCR> middlewareCRList = middlewareCRService.listCR(clusterId, null, null);
            middlewareCRListMap.put(clusterId, middlewareCRList);
        }
        Map<String, List<BeanProjectNamespace>> beanProjectNamespaceListMap =
            beanProjectNamespaceList.stream().collect(Collectors.groupingBy(BeanProjectNamespace::getProjectId));
        List<ProjectDto> projectDtoList = new ArrayList<>();
        for (String key : beanProjectNamespaceListMap.keySet()) {
            ProjectDto projectDto = new ProjectDto();
            projectDto.setProjectId(key);
            int count = 0;
            for (String clusterId : middlewareCRListMap.keySet()) {
                for (MiddlewareCR middlewareCr : middlewareCRListMap.get(clusterId)) {
                    if (beanProjectNamespaceListMap.get(key).stream()
                        .anyMatch(beanProjectNamespace -> beanProjectNamespace.getNamespace()
                            .equals(middlewareCr.getMetadata().getNamespace())
                            && beanProjectNamespace.getClusterId().equals(clusterId))) {
                        count = count + 1;
                    }
                }
            }
            projectDto.setMiddlewareCount(count);
            projectDtoList.add(projectDto);
        }
        return projectDtoList;
    }

    @Override
    public ProjectDto findProjectByNamespace(String namespace) {
        QueryWrapper<BeanProjectNamespace> wrapper =
            new QueryWrapper<BeanProjectNamespace>().eq("namespace", namespace);
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);

        ProjectDto projectDto = new ProjectDto();
        if (!CollectionUtils.isEmpty(beanProjectNamespaceList)) {
            String projectId = beanProjectNamespaceList.get(0).getProjectId();
            QueryWrapper<BeanProject> projectWrapper = new QueryWrapper<BeanProject>().eq("project_id", projectId);
            List<BeanProject> beanProjectList = beanProjectMapper.selectList(projectWrapper);
            if (!CollectionUtils.isEmpty(beanProjectList)) {
                BeanUtils.copyProperties(beanProjectList.get(0), projectDto);
            }
        }
        return projectDto;
    }

    @Override
    public BeanProject get(String projectId) {
        QueryWrapper<BeanProject> wrapper = new QueryWrapper<BeanProject>().eq("project_id", projectId);
        List<BeanProject> beanProjectList = beanProjectMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(beanProjectList)) {
            return null;
        }
        return beanProjectList.get(0);
    }

    @Override
    public void bindNamespace(Namespace namespace) {
        QueryWrapper<BeanProjectNamespace> wrapper = new QueryWrapper<BeanProjectNamespace>()
            .eq("namespace", namespace.getName()).eq("cluster_id", namespace.getClusterId());
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(beanProjectNamespaceList)) {
            throw new BusinessException(ErrorMessage.PROJECT_NAMESPACE_ALREADY_BIND);
        }
        AssertUtil.notBlank(namespace.getProjectId(), DictEnum.PROJECT_ID);
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
    public List<String> getClusters(String projectId) {
        List<Namespace> namespaceList = this.getNamespace(projectId, null, false);
        return namespaceList.stream().map(Namespace::getClusterId).collect(Collectors.toList());
    }

    @Override
    public List<Namespace> getNamespace(String projectId) {
        return getNamespace(projectId, null, false);
    }

    @Override
    public List<Namespace> getNamespace(String projectId, String clusterId, Boolean withQuota) {
        QueryWrapper<BeanProjectNamespace> wrapper =
            new QueryWrapper<BeanProjectNamespace>().eq("project_id", projectId);
        if (!StringUtils.isEmpty(clusterId)) {
            wrapper.eq("cluster_id", clusterId);
        }
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);
        List<Namespace> namespaces = beanProjectNamespaceList.stream().map(beanProjectNamespace -> {
            Namespace namespace = new Namespace();
            BeanUtils.copyProperties(beanProjectNamespace, namespace);
            namespace.setClusterAliasName(clusterService.findById(namespace.getClusterId()).getNickname());
            namespace.setName(beanProjectNamespace.getNamespace());
            if (StringUtils.isEmpty(namespace.getAliasName())) {
                namespace.setAliasName(beanProjectNamespace.getNamespace());
            }
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

        if (StringUtils.isEmpty(clusterId)) {
            return namespaces;
        }
        return setAvailableDomainStatus(namespaces, clusterId);
    }

    public void checkParam(ProjectDto projectDto){
        QueryWrapper<BeanProject> wrapper = new QueryWrapper<BeanProject>().eq("name", projectDto.getName());
        List<BeanProject> beanProjectList = beanProjectMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(beanProjectList)){
            throw new BusinessException(ErrorMessage.PROJECT_NAME_EXIST);
        }
    }

    /**
     * 校验项目是否存在
     */
    public BeanProject checkExist(String projectId) {
        QueryWrapper<BeanProject> wrapper = new QueryWrapper<BeanProject>().eq("project_id", projectId);
        List<BeanProject> beanProjectList = beanProjectMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(beanProjectList)) {
            throw new BusinessException(ErrorMessage.PROJECT_NOT_EXIST);
        }
        return beanProjectList.get(0);
    }

    /**
     * 设置双活分区状态
     *
     * @param namespaces
     * @param clusterId
     * @return
     */
    public List<Namespace> setAvailableDomainStatus(List<Namespace> namespaces, String clusterId) {
        try {
            List<Namespace> nsList = namespaceService.list(clusterId, false, null);
            Map<String, Boolean> nsMap = new HashMap<>();
            nsList.forEach(ns -> {
                nsMap.put(ns.getName(), ns.isAvailableDomain());
            });
            namespaces.forEach(ns -> {
                ns.setAvailableDomain(nsMap.get(ns.getName()));
            });
            return namespaces;
        } catch (Exception e) {
            log.error("查询双活分区状态失败", e);
            return namespaces;
        }
    }

    private void checkAndBindImagePullSecret(String clusterId, String namespace) {
        ServiceAccount serviceAccount = serviceAccountService.get(clusterId, namespace, middlewareServiceAccount);
        List<ImageRepositoryDTO> imageRepositoryDTOS = imageRepositoryService.list(clusterId);
        imageRepositoryService.createImagePullSecret(clusterId, namespace, imageRepositoryDTOS);
        List<Secret> allImagePullSecret = imageRepositoryService.listImagePullSecret(clusterId, namespace);
        serviceAccountService.bindImagePullSecret(clusterId, namespace, serviceAccount, allImagePullSecret);
    }

}
