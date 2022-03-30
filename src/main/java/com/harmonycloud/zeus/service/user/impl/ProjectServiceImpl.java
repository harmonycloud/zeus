package com.harmonycloud.zeus.service.user.impl;

import static com.harmonycloud.caas.common.constants.user.UserConstant.USERNAME;

import java.util.*;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.user.UserRole;
import com.harmonycloud.zeus.bean.user.BeanUserRole;
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

    @Override
    public void add(ProjectDto projectDto) {
        if(StringUtils.isEmpty(projectDto.getUser())){
            projectDto.setUser("admin");
        }
        String projectId = UUIDUtils.get16UUID();
        BeanProject beanProject = new BeanProject();
        BeanUtils.copyProperties(projectDto, beanProject);
        beanProject.setProjectId(projectId);
        beanProject.setCreateTime(new Date());
        //绑定用户角色
        userRoleService.insert(projectId, projectDto.getUser(), 2);
        // 绑定分区
        if (projectDto.getClusterList() != null) {
            projectDto.getClusterList().forEach(cluster -> {
                if (cluster.getNamespaceList() != null) {
                    cluster.getNamespaceList()
                        .forEach(namespace -> this.bindNamespace(namespace.setProjectId(projectId)));
                }
            });
        }
        // 添加项目
        beanProjectMapper.insert(beanProject);
    }

    @Override
    public List<ProjectDto> list() {
        QueryWrapper<BeanProject> wrapper = new QueryWrapper<>();
        List<BeanProject> beanProjectList = beanProjectMapper.selectList(wrapper);
        CurrentUser currentUser = CurrentUserRepository.getUserExistNull();
        JSONObject user = JwtTokenComponent.checkToken(currentUser.getToken()).getValue();
        if (!userRoleService.checkAdmin(user.getString(USERNAME))) {
            beanProjectList = beanProjectList.stream().filter(bp -> (StringUtils.isNotEmpty(bp.getUser())
                && Arrays.asList(bp.getUser().split(",")).contains(USERNAME))).collect(Collectors.toList());
        }
        // 获取项目分区
        QueryWrapper<BeanProjectNamespace> nsWrapper = new QueryWrapper<>();
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(nsWrapper);
        Map<String, List<BeanProjectNamespace>> beanProjectNamespaceListMap =
            beanProjectNamespaceList.stream().collect(Collectors.groupingBy(BeanProjectNamespace::getProjectId));

        List<ProjectDto> projectDtoList = new ArrayList<>();
        beanProjectList.forEach(beanProject -> {
            ProjectDto projectDto = new ProjectDto();
            BeanUtils.copyProperties(beanProject, projectDto);
            if (StringUtils.isNotEmpty(beanProject.getUser())) {
                projectDto.setMemberCount(beanProject.getUser().split(",").length);
            }
            if (beanProjectNamespaceListMap.containsKey(projectDto.getProjectId())){
                projectDto.setNamespaceCount(beanProjectNamespaceListMap.get(projectDto.getProjectId()).size());
            }
            projectDtoList.add(projectDto);
        });
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
    public List<UserDto> getUser(String projectId) {
        BeanProject beanProject = checkExist(projectId);
        List<String> usernameList = Arrays.asList(beanProject.getUser().split(","));
        List<UserDto> userDtoList = userService.list(null);
        userDtoList = userDtoList.stream()
            .filter(userDto -> usernameList.stream().anyMatch(username -> userDto.getUserName().equals(username)))
            .collect(Collectors.toList());

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
            sb.append(",").append(userDto.getUserName());
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
        BeanProject beanProject = checkExist(projectId);
        beanProject.setUser(beanProject.getUser().replace("," + username, ""));
        beanProjectMapper.updateById(beanProject);
        userRoleService.delete(username, projectId);
    }

    @Override
    public void delete(String projectId) {
        //todo 有服务存在是否允许删除  同步将用户和分区解绑
        QueryWrapper<BeanProject> wrapper = new QueryWrapper<BeanProject>().eq("projectId", projectId);
        beanProjectMapper.delete(wrapper);
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
            new QueryWrapper<BeanProjectNamespace>().eq("namespace", namespace.getName());
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
    public void unBindNamespace(String projectId, String namespace) {

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
