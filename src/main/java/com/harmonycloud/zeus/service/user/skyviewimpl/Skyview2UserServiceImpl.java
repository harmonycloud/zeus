package com.harmonycloud.zeus.service.user.skyviewimpl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.caas.common.model.ProjectDTO;
import com.middleware.caas.common.model.user.RoleDto;
import com.middleware.caas.common.model.user.UserDto;
import com.middleware.caas.common.model.user.UserRole;
import com.harmonycloud.zeus.bean.user.BeanUserRole;
import com.harmonycloud.zeus.service.user.ProjectService;
import com.harmonycloud.zeus.service.user.RoleService;
import com.harmonycloud.zeus.service.user.UserRoleService;
import com.harmonycloud.zeus.service.user.impl.UserServiceImpl;
import com.harmonycloud.zeus.skyviewservice.client.Skyview2ProjectServiceClient;
import com.harmonycloud.zeus.skyviewservice.client.Skyview2UserServiceClient;
import com.harmonycloud.zeus.util.ZeusCurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2022/6/8 1:50 下午
 */
@Slf4j
@Service
@ConditionalOnProperty(value="system.usercenter",havingValue = "skyview2")
public class Skyview2UserServiceImpl extends UserServiceImpl {

    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private Skyview2UserServiceClient skyviewUserService;
    @Autowired
    private Skyview2ProjectServiceClient projectServiceClient;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private Skyview2ProjectServiceImpl skyview2ProjectService;

    @Override
    public UserDto getUserDto(String userName, String projectId) {
        if (StringUtils.isEmpty(userName)) {
            userName = getUsername();
        }
        synchronized (this){
            syncCurrentUserInfo();
        }
        // 如果用户名不为空，则查询当前登录用户的信息(即：用户在每个项目下的角色)
        UserDto userDto = new UserDto();
        userDto.setIsAdmin(ZeusCurrentUser.isAdmin());
        userDto.setUserName(ZeusCurrentUser.getUserName());
        userDto.setAliasName(ZeusCurrentUser.getNickName());
        List<UserRole> userRoleList = userRoleService.get(userName);
        if (!CollectionUtils.isEmpty(userRoleList)) {
            userDto.setUserRoleList(userRoleList);
        }
        return userDto;
    }

    @Override
    public UserDto getUserDto(String userName) {
        UserDto userDto = new UserDto();
        userDto.setUserName(userName);
        super.setUserRoleList(userName, userDto);
        if (!CollectionUtils.isEmpty(userDto.getUserRoleList())) {
            userDto.setIsAdmin(userDto.getUserRoleList().stream().anyMatch(userRole -> userRole.getRoleId() == 1));
        }
        return userDto;
    }

    @Override
    public List<UserDto> list(String keyword) {
        CaasResult<JSONArray> userResult = skyviewUserService.listUser(ZeusCurrentUser.getCaasToken(), keyword);
        JSONArray userData = userResult.getData();
        return convertUserData(userData);
    }



    public List<UserDto> convertUserData(JSONArray userData) {
        ArrayList<UserDto> userDtos = new ArrayList<>();
        userData.forEach(user -> {
            JSONObject userJson = (JSONObject) user;
            UserDto userDto = new UserDto();
            String username = userJson.getString("username");
            userDto.setId(userJson.getInteger("id"));
            userDto.setUserName(username);
            userDto.setAliasName(userJson.getString("realName"));
            userDto.setIsAdmin(userJson.getBoolean("admin"));
            userDto.setPhone(userJson.getString("phone"));
            if (userJson.containsKey("createTime")) {
                userDto.setCreateTime(userJson.getDate("createTime"));
            }
            if (Boolean.FALSE.equals(userJson.getBoolean("admin"))) {
                JSONArray otherProjects = userJson.getJSONArray("otherProjects");
                userDto.setUserRoleList(convertUserRole(otherProjects, username));
            } else {
                RoleDto roleDto = roleService.get(1);
                UserRole userRole = new UserRole();
                userRole.setRoleId(roleDto.getId());
                userRole.setRoleName(roleDto.getName());
                ArrayList<UserRole> userRoles = new ArrayList<>();
                userRoles.add(userRole);
                userDto.setUserRoleList(userRoles);
            }
            userDtos.add(userDto);
        });
        return userDtos;
    }

    /**
     * 转换得到用户在其他项目的角色
     * @param otherProject
     * @param username
     * @return
     */
    private List<UserRole> convertUserRole(JSONArray otherProject, String username) {
        ArrayList<UserRole> userRoles = new ArrayList<>();
        Set<String> projectSet = new HashSet<>();
        List<JSONObject> listProjectRole  = new ArrayList<>();
        List<JSONObject> filteredProject = new ArrayList<>();
        // 过滤掉没有项目id的项目角色
        otherProject.forEach(projectRole -> {
            JSONObject projectJson = (JSONObject) projectRole;
            String projectId = projectJson.getString("projectId");
            if (projectId != null) {
                listProjectRole.add((JSONObject) projectRole);
            }
        });
        // 过滤掉项目管理员的项目成员角色，因为当用户为项目管理员时，也可能拥有成员角色
        Map<String, List<JSONObject>> projectMap = listProjectRole.stream().collect(Collectors.groupingBy(item -> item.getString("projectId")));
        projectMap.forEach((k, v) -> {
            if (v.size() > 1) {
                filteredProject.add(v.stream().sorted(Comparator.comparingInt(o -> o.getInteger("roleId"))).collect(Collectors.toList()).get(0));
            } else {
                filteredProject.addAll(v);
            }
        });

        filteredProject.forEach(projectRole -> {
            String projectId = projectRole.getString("projectId");
            if (projectId != null) {
                projectSet.add(projectId);
                UserRole userRole = new UserRole();
                String roleId = projectRole.getString("roleId");
                if ("2".equals(roleId) || "3".equals(roleId)) {
                    saveUserProjectRole(projectId, 2, username);
                    userRole.setRoleId(2);
                    RoleDto roleDto = roleService.get(2);
                    userRole.setRoleName(roleDto.getName());
                } else {
                    saveUserProjectRole(projectId, 4, username);
                    BeanUserRole beanUserRole = userRoleService.get(username, projectId);
                    RoleDto roleDto = roleService.get(beanUserRole.getRoleId());
                    userRole.setRoleName(roleDto.getName());
                    userRole.setRoleId(roleDto.getId());
                }
                userRole.setUserName(username);
                userRole.setProjectId(projectId);
                userRole.setProjectName(projectRole.getString("projectName"));
                userRoles.add(userRole);
            }
        });
        if (userRoles.isEmpty()) {
            return null;
        }
        return userRoles;
    }

    /**
     * 同步当前用户角色、项目、项目分区信息
     */
    public void syncCurrentUserInfo(){
        String username = ZeusCurrentUser.getUserName();
        String caasToken = ZeusCurrentUser.getCaasToken();
        List<ProjectDTO>  projects =  skyview2ProjectService.listAllTenantProject(caasToken);
        if (CollectionUtils.isEmpty(projects)) {
            return;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(projects.size());
        CountDownLatch latch = new CountDownLatch(projects.size());
        // 2、获取用户在每个项目下的角色
        projects.forEach(project -> {
            executorService.submit(()->{
                try {
                    String projectId = project.getProjectId();
                    CaasResult<JSONArray> projectRoleResult = projectServiceClient.getUserProjectRole(caasToken, projectId);
                    if (projectRoleResult.getData() != null) {
                        Integer userRoleId = projectRoleResult.getJSONArrayIntegerVal(0, "id");
                        project.setUserRoleId(userRoleId);
                    }
                } catch (Exception e) {
                    log.error("查询用户项目角色失败了", e);
                } finally {
                    latch.countDown();
                }
            });
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("查询用户项目角色出错了", e);
        } finally {
            executorService.shutdown();
        }

        // 3、保存项目分区绑定信息
        projects.forEach(project -> {
            projectService.bindNamespace(project.getNamespaces());
        });

        // 4、判断是否是超级管理员，若是，则判断是否已存储该用户角色，没有则存储 => 超级管理员，超级管理员拥有所有项目权限
        if (ZeusCurrentUser.isAdmin()) {
            List<UserRole> userRoles = userRoleService.get(username);
            if (CollectionUtils.isEmpty(userRoles)) {
                userRoleService.insert(null, username, 1);
            } else if (userRoles.size() == 1) {
                UserRole userRole = userRoles.get(0);
                if (userRole.getRoleId() != 1) {
                    userRoleService.delete(username, null, null);
                    userRoleService.insert(null, username, 1);
                }
            } else {
                userRoleService.delete(username, null, null);
                userRoleService.insert(null, username, 1);
            }
        } else {
            // 删除管理员角色
            userRoleService.delete(username, null, 1);
            // 删除项目不存在的角色信息
            List<String> projectIds = projects.stream().map(ProjectDTO::getProjectId).collect(Collectors.toList());
            userRoleService.deleteRedundantRole(username, projectIds);
            // 5、判断用户在每个项目下的角色是否是租户管理员，若是，则判断是否已存储该用户每个项目项目管理员角色，没有则存储 => 所有项目项目管理员
            for (ProjectDTO project : projects) {
                if (project.getUserRoleId() == null) {
                    continue;
                }
                if (project.getUserRoleId().equals(2) || project.getUserRoleId().equals(3)) {
                    // 是租户管理员或项目管理员，存储为项目管理员 2
                    saveUserProjectRole(project.getProjectId(), 2, username);
                } else {
                    // 若非以上角色，则是普通成员，判断是否存在，不存在则新增普通成员角色 4
                    saveUserProjectRole(project.getProjectId(), 4, username);
                }
            }
        }
    }

    /**
     * 保存用户项目角色信息
     * @param projectId
     * @param userRoleId
     * @param username
     */
    public void saveUserProjectRole(String projectId, int userRoleId, String username) {
        Integer roleId = userRoleService.getRoleId(username, projectId);
        if (roleId == null) {
            userRoleService.insert(projectId, username, userRoleId);
            return;
        }
        if (!roleId.equals(userRoleId)) {
            if (userRoleId == 2) {
                userRoleService.delete(username, projectId, null);
                userRoleService.insert(projectId, username, userRoleId);
            } else {
                userRoleService.delete(username, projectId, 2);
                if (!userRoleService.checkExistsNormalRole(username)) {
                    userRoleService.insert(projectId, username, userRoleId);
                }
            }
        }
    }


}
