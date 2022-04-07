package com.harmonycloud.zeus.service.user.impl;

import static com.harmonycloud.caas.common.constants.user.UserConstant.USERNAME;

import java.util.*;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareResourceInfo;
import com.harmonycloud.caas.common.model.middleware.ProjectMiddlewareResourceInfo;
import com.harmonycloud.caas.common.model.user.UserRole;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRService;
import com.harmonycloud.zeus.service.k8s.NamespaceService;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import com.harmonycloud.zeus.service.user.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.caas.common.model.user.ProjectDto;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import com.harmonycloud.caas.filters.user.CurrentUser;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.bean.user.BeanProject;
import com.harmonycloud.zeus.bean.user.BeanProjectNamespace;
import com.harmonycloud.zeus.dao.user.BeanProjectMapper;
import com.harmonycloud.zeus.dao.user.BeanProjectNamespaceMapper;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.user.ProjectService;
import com.harmonycloud.zeus.service.user.UserRoleService;
import com.harmonycloud.zeus.util.AssertUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author xutianhong
 * @Date 2022/3/24 9:25 上午
 */
@Service
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private BeanProjectMapper beanProjectMapper;
    @Autowired
    private BeanProjectNamespaceMapper beanProjectNamespaceMapper;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private UserService userService;
    @Autowired
    private MiddlewareCRService middlewareCRService;
    @Autowired
    private MiddlewareInfoService middlewareInfoService;

    @Override
    public void add(ProjectDto projectDto) {
        AssertUtil.notBlank(projectDto.getName(), DictEnum.PROJECT_NAME);
        String projectId = UUIDUtils.get16UUID();
        BeanProject beanProject = new BeanProject();
        BeanUtils.copyProperties(projectDto, beanProject);
        beanProject.setProjectId(projectId);
        beanProject.setCreateTime(new Date());
        // 添加项目
        beanProjectMapper.insert(beanProject);
        //绑定用户角色
        if (StringUtils.isNotEmpty(projectDto.getUser())){
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
    public List<ProjectDto> list(String key) {
        QueryWrapper<BeanProject> wrapper = new QueryWrapper<>();
        List<BeanProject> beanProjectList = beanProjectMapper.selectList(wrapper);
        CurrentUser currentUser = CurrentUserRepository.getUserExistNull();
        JSONObject user = JwtTokenComponent.checkToken(currentUser.getToken()).getValue();
        // 获取用户角色对应
        UserDto userDto = userService.list(null).stream().filter(u -> u.getUserName().equals(user.getString(USERNAME)))
            .collect(Collectors.toList()).get(0);
        Map<String, UserRole> userRoleMap = userDto.getUserRoleList().stream().collect(Collectors.toMap(UserRole::getProjectId, u -> u));
        // 是否为 admin
        boolean flag = userDto.getUserRoleList().stream().anyMatch(userRole -> userRole.getRoleId() == 1);
        // 非admin,进行过滤
        if (!flag) {
            beanProjectList = beanProjectList.stream()
                .filter(bp -> StringUtils.isNotEmpty(bp.getUser())
                    && Arrays.asList(bp.getUser().split(",")).contains(user.getString(USERNAME)))
                .collect(Collectors.toList());
        }
        // 获取项目分区
        QueryWrapper<BeanProjectNamespace> nsWrapper = new QueryWrapper<>();
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(nsWrapper);
        Map<String, List<BeanProjectNamespace>> beanProjectNamespaceListMap =
            beanProjectNamespaceList.stream().collect(Collectors.groupingBy(BeanProjectNamespace::getProjectId));

        // 封装数据
        List<ProjectDto> projectDtoList = new ArrayList<>();
        for (BeanProject beanProject : beanProjectList) {
            ProjectDto projectDto = new ProjectDto();
            BeanUtils.copyProperties(beanProject, projectDto);
            if (StringUtils.isNotEmpty(beanProject.getUser())) {
                projectDto.setMemberCount(beanProject.getUser().split(",").length);
            }
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
        if (StringUtils.isNotEmpty(key)) {
            projectDtoList = projectDtoList.stream()
                .filter(projectDto -> (StringUtils.isNotEmpty(projectDto.getAliasName())
                    && projectDto.getAliasName().contains(key))
                    || (StringUtils.isNotEmpty(projectDto.getDescription())
                        && projectDto.getDescription().contains(key)))
                .collect(Collectors.toList());
        }

        return projectDtoList;
    }

    @Override
    public List<Namespace> getNamespace(String projectId) {
        QueryWrapper<BeanProjectNamespace> wrapper =
            new QueryWrapper<BeanProjectNamespace>().eq("project_id", projectId);
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);

        return beanProjectNamespaceList.stream().map(beanProjectNamespace -> {
            Namespace namespace = new Namespace();
            BeanUtils.copyProperties(beanProjectNamespace, namespace);
            namespace.setClusterAliasName(clusterService.findById(namespace.getClusterId()).getNickname());
            namespace.setName(beanProjectNamespace.getNamespace());
            return namespace;
        }).collect(Collectors.toList());
    }

    @Override
    public List<MiddlewareClusterDTO> getAllocatableNamespace(String projectId) {
        List<MiddlewareClusterDTO> clusterList = clusterService.listClusters(true, null);
        QueryWrapper<BeanProjectNamespace> wrapper = new QueryWrapper<>();
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);
        Map<String, List<BeanProjectNamespace>> nsMap =
            beanProjectNamespaceList.stream().collect(Collectors.groupingBy(BeanProjectNamespace::getClusterId));
        clusterList.forEach(cluster -> {
            List<Namespace> list = cluster.getNamespaceList().stream()
                .filter(ns -> !nsMap.containsKey(ns.getClusterId())
                    || nsMap.get(cluster.getId()).stream().noneMatch(pNs -> pNs.getNamespace().equals(ns.getName())))
                .collect(Collectors.toList());
            cluster.setNamespaceList(list);
        });
        clusterList.removeIf(cluster -> CollectionUtils.isEmpty(cluster.getNamespaceList()));
        return clusterList;
    }

    @Override
    public List<UserDto> getUser(String projectId, Boolean allocatable) {
        BeanProject beanProject = checkExist(projectId);
        List<String> usernameList = Arrays.asList(beanProject.getUser().split(","));
        List<UserDto> userDtoList = userService.list(null);
        if (allocatable) {
            // 获取可分配的
            userDtoList = userDtoList.stream()
                .filter(userDto -> usernameList.stream().noneMatch(username -> userDto.getUserName().equals(username)))
                .filter(userDto -> CollectionUtils.isEmpty(userDto.getUserRoleList())
                    || userDto.getUserRoleList().stream().noneMatch(userRole -> userRole.getRoleId() == 1))
                .collect(Collectors.toList());
        } else {
            // 获取已分配的
            userDtoList = userDtoList.stream()
                .filter(userDto -> usernameList.stream().anyMatch(username -> userDto.getUserName().equals(username)))
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
        BeanProject beanProject = checkExist(projectDto.getProjectId());
        StringBuilder sb = new StringBuilder();
        projectDto.getUserDtoList().forEach(userDto -> {
            if (StringUtils.isNotEmpty(userDto.getUserName())){
                sb.append(",");
            }
            sb.append(userDto.getUserName());
            userRoleService.insert(projectDto.getProjectId(), userDto.getUserName(), userDto.getRoleId());
        });
        beanProject.setUser(beanProject.getUser() + sb.toString());
        beanProjectMapper.updateById(beanProject);
    }

    @Override
    public void updateUserRole(String projectId, UserDto userDto) {
        userRoleService.update(userDto, projectId);
    }

    @Override
    public void unbindUser(String projectId, String username) {
        if (StringUtils.isNotEmpty(username)){
            BeanProject beanProject = checkExist(projectId);
            beanProject.setUser(beanProject.getUser().replace("," + username, ""));
            beanProjectMapper.updateById(beanProject);
        }
        userRoleService.delete(username, projectId);
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
    public void bindNamespace(Namespace namespace) {
        QueryWrapper<BeanProjectNamespace> wrapper =
            new QueryWrapper<BeanProjectNamespace>().eq("namespace", namespace.getName()).eq("cluster_id", namespace.getClusterId());
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
    }

    @Override
    public void unBindNamespace(String projectId, String clusterId, String namespace) {
        QueryWrapper<BeanProjectNamespace> wrapper = new QueryWrapper<BeanProjectNamespace>()
            .eq("project_id", projectId);
        if (StringUtils.isNotEmpty(clusterId)){
            wrapper.eq("cluster_id", clusterId);
        }
        if (StringUtils.isNotEmpty(namespace)){
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
        Set<String> cluster = new HashSet<>();
        beanProjectNamespaceList.forEach(beanProjectNamespace -> {
            cluster.add(beanProjectNamespace.getClusterId());
        });
        // 查询数据
        List<MiddlewareResourceInfo> all = new ArrayList<>();
        for (String clusterId : cluster) {
            all.addAll(clusterService.getMwResource(clusterId));
        }
        // 根据分区过滤
        all = all.stream().filter(middlewareResourceInfo -> beanProjectNamespaceList.stream().anyMatch(
            beanProjectNamespace -> beanProjectNamespace.getNamespace().equals(middlewareResourceInfo.getNamespace())))
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(all)) {
            return new ArrayList<>();
        }
        // 获取image.path
        Map<String,
            String> middlewareImagePathMap = middlewareInfoService.list(false).stream()
                .filter(beanMiddlewareInfo -> beanMiddlewareInfo.getImagePath() != null)
                .collect(Collectors.toMap(BeanMiddlewareInfo::getChartName, BeanMiddlewareInfo::getImagePath));
        // 封装数据
        Map<String, List<MiddlewareResourceInfo>> map =
            all.stream().collect(Collectors.groupingBy(MiddlewareResourceInfo::getType));
        List<ProjectMiddlewareResourceInfo> infoList = new ArrayList<>();
        for (String key : map.keySet()) {
            ProjectMiddlewareResourceInfo projectMiddlewareResourceInfo = new ProjectMiddlewareResourceInfo()
                .setType(key).setAliasName(MiddlewareOfficialNameEnum.findByMiddlewareName(key))
                .setMiddlewareResourceInfoList(map.get(key))
                .setImagePath(middlewareImagePathMap.getOrDefault(key, null));
            infoList.add(projectMiddlewareResourceInfo);
        }
        return infoList;
    }

    @Override
    public List<String> getClusters(String projectId) {
        List<Namespace> namespaceList = this.getNamespace(projectId);
        return namespaceList.stream().map(Namespace::getClusterId).collect(Collectors.toList());
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
        // 校验角色权限
        Boolean isAdmin = userRoleService.checkAdmin(username);
        if (!isAdmin) {
            QueryWrapper<BeanProject> projectQueryWrapper = new QueryWrapper<>();
            List<BeanProject> beanProjectList = beanProjectMapper
                .selectList(projectQueryWrapper).stream().filter(beanProject -> Arrays
                    .asList(beanProject.getUser().split(",")).stream().anyMatch(name -> name.equals(username)))
                .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(beanProjectList)) {
                beanProjectNamespaceList = beanProjectNamespaceList.stream()
                    .filter(beanProjectNamespace -> beanProjectList.stream().anyMatch(
                        beanProject -> beanProject.getProjectId().equals(beanProjectNamespace.getProjectId())))
                    .collect(Collectors.toList());
            }
        }
        if (CollectionUtils.isEmpty(beanProjectNamespaceList)) {
            return null;
        }
        Set<String> clusterIdList =
            beanProjectNamespaceList.stream().map(BeanProjectNamespace::getClusterId).collect(Collectors.toSet());
        Map<String, List<MiddlewareCR>> middlewareCRListMap = new HashMap<>();
        for (String clusterId : clusterIdList) {
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

    /**
     * 校验项目是否存在
     */
    public BeanProject checkExist(String projectId){
        QueryWrapper<BeanProject> wrapper = new QueryWrapper<BeanProject>().eq("project_id", projectId);
        List<BeanProject> beanProjectList = beanProjectMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(beanProjectList)){
            throw new BusinessException(ErrorMessage.PROJECT_NOT_EXIST);
        }
        return beanProjectList.get(0);
    }

}
