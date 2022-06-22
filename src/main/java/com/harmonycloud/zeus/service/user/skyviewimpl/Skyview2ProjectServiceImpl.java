package com.harmonycloud.zeus.service.user.skyviewimpl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.CaasResult;
import com.harmonycloud.caas.common.model.ProjectDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.caas.common.model.user.ProjectDto;
import com.harmonycloud.caas.common.model.user.RoleDto;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.zeus.bean.user.BeanProjectNamespace;
import com.harmonycloud.zeus.bean.user.BeanUserRole;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.user.RoleService;
import com.harmonycloud.zeus.service.user.UserRoleService;
import com.harmonycloud.zeus.service.user.impl.ProjectServiceImpl;
import com.harmonycloud.zeus.skyviewservice.Skyview2ProjectServiceClient;
import com.harmonycloud.zeus.skyviewservice.Skyview2UserServiceClient;
import com.harmonycloud.zeus.util.ZeusCurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2022/6/14 5:28 下午
 */
@Slf4j
@Service
@ConditionalOnProperty(value="system.usercenter",havingValue = "skyview2")
public class Skyview2ProjectServiceImpl extends ProjectServiceImpl {

    @Autowired
    private Skyview2ProjectServiceClient projectServiceClient;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private Skyview2UserServiceClient userServiceClient;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private RoleService roleService;

    /**
     * 项目租户id缓存 key:项目id value:租户id
     */
    private static Map<String,String> projectTenantCache = new HashMap<>();

    /**
     * 获取观云台项目列表
     * @param caastoken
     * @return
     */
    public List<ProjectDTO> listAllTenantProject(String caastoken) {
        // 1、获取当前用户所有租户
        CaasResult<JSONObject> currentResult = userServiceClient.current(caastoken, true);
        JSONArray tenants = currentResult.getJSONArray("tenants");
        // 2、获取所有租户所有项目
        List<ProjectDTO> projects = new ArrayList<>();
        for (Object tenant : tenants) {
            JSONObject jsonTenant = (JSONObject) tenant;
            CaasResult<JSONArray> projectResult = projectServiceClient.getTenantProject(caastoken, jsonTenant.getString("tenantId"));
            if (Boolean.TRUE.equals(projectResult.getSuccess())) {
                JSONArray projectList = projectResult.getData();
                projects.addAll(convertProject(projectList, jsonTenant.getString("tenantName"), jsonTenant.getString("aliasName"), caastoken));
            }
        }
        return projects;
    }

    /**
     * 转换得到项目集合
     * @param projects
     * @param tenantName
     * @return
     */
    private  List<ProjectDTO> convertProject(JSONArray projects, String tenantName, String tenantAliasName, String caastoken) {
        List<ProjectDTO> projectDTOList = new ArrayList<>();
        projects.forEach(project -> {
            JSONObject jsonProject = (JSONObject) project;
            ProjectDTO projectDTO = new ProjectDTO();
            projectDTO.setProjectId(jsonProject.getString("projectId"));
            projectDTO.setProjectName(jsonProject.getString("projectName"));
            projectDTO.setDescription(jsonProject.getString("annotation"));
            projectDTO.setProjectAliasName(((JSONObject) project).getString("aliasName"));
            projectDTO.setTenantId(jsonProject.getString("tenantId"));
            projectDTO.setCreateTime(jsonProject.getDate("createTime"));
            projectDTO.setTenantName(tenantName);
            projectDTO.setTenantAliasName(tenantAliasName);
            projectDTO.setNamespaces(convertProjectNamespace(jsonProject.getJSONArray("namespaceList"), projectDTO.getProjectId()));
            projectDTO.setNamespaceCount(projectDTO.getNamespaces().size());
            // 查询项目成员
            CaasResult<JSONObject> projectMemberResult = projectServiceClient.getProjectMember(caastoken, projectDTO.getTenantId(), projectDTO.getProjectId());
            JSONArray userDataList = projectMemberResult.getJSONArray("userDataList");
            if (userDataList != null) {
                projectDTO.setUserDtos(convertProjectMember(userDataList, projectDTO.getProjectId()));
                projectDTO.setMemberCount(projectDTO.getUserDtos().size());
            } else {
                projectDTO.setMemberCount(0);
            }
            projectTenantCache.put(projectDTO.getProjectId(), projectDTO.getTenantId());
            projectDTOList.add(projectDTO);
        });
        return projectDTOList;
    }

    /**
     * 转换得到项目的命名空间列表
     * @param namespaceList
     * @param projectId
     * @return
     */
    private List<Namespace> convertProjectNamespace(JSONArray namespaceList, String projectId) {
        ArrayList<Namespace> namespaces = new ArrayList<>();
        if (!CollectionUtils.isEmpty(namespaceList)) {
            namespaceList.forEach(namespace -> {
                JSONObject jsonNamespace = (JSONObject) namespace;
                Namespace ns = new Namespace();
                ns.setName(jsonNamespace.getString("namespaceName"));
                ns.setProjectId(projectId);
                ns.setAliasName(jsonNamespace.getString("aliasName"));
                JSONArray clusters = jsonNamespace.getJSONArray("clusters");
                if (clusters != null && !clusters.isEmpty()) {
                    JSONObject jsonCluster = (JSONObject) clusters.get(0);
                    ns.setClusterId(clusterService.convertClusterId(jsonCluster.getString("id")));
                } else {
                    ns.setClusterId(clusterService.convertClusterId(jsonNamespace.getString("clusterId")));
                }
                namespaces.add(ns);
            });
        }
        return namespaces;
    }

    /**
     * 转换得到项目成员
     * @param jsonArray
     * @return
     */
    private List<UserDto> convertProjectMember(JSONArray jsonArray, String projectId) {
        List<UserDto> userDtos = new ArrayList<>();
        if (jsonArray == null) {
            return userDtos;
        }
        for (Object o : jsonArray) {
            JSONObject jsonUser = (JSONObject) o;
            UserDto userDto = new UserDto();
            userDto.setUserName(jsonUser.getString("username"));
            userDto.setAliasName(jsonUser.getString("nickName"));
            userDto.setEmail(jsonUser.getString("email"));
            JSONObject role = jsonUser.getJSONObject("role");
            if (role != null) {
                int id = role.getInteger("id");
                if (id == 2 || id == 3) {
                    userDto.setRoleId(2);
                    userDto.setRoleName("项目管理员");
                } else if (id > 3) {
                    BeanUserRole beanUserRole = userRoleService.get(userDto.getUserName(), projectId);
                    if (beanUserRole == null) {
                        userRoleService.insert(projectId, userDto.getUserName(), 3);
                        beanUserRole = userRoleService.get(userDto.getUserName(), projectId);
                    }
                    RoleDto roleDto = roleService.get(beanUserRole.getRoleId());
                    userDto.setRoleId(beanUserRole.getRoleId());
                    userDto.setRoleName(roleDto.getName());
                } else {
                    continue;
                }
            }
            userDtos.add(userDto);
        }
        return userDtos;
    }

    /**
     * 查询项目分区
     * @param tenantId
     * @param projectId
     * @return
     */
    private JSONArray listProjectNamespace(String tenantId, String projectId) {
        CaasResult<JSONArray> projectNamespace = projectServiceClient.getProjectNamespace(ZeusCurrentUser.getCaasToken(), tenantId, projectId);
        CaasResult<JSONArray> federationProjectNamespace = projectServiceClient.getFederationProjectNamespace(ZeusCurrentUser.getCaasToken(), tenantId, projectId);
        JSONArray projectAry = new JSONArray();
        if (projectNamespace.getData() != null && !projectNamespace.getData().isEmpty()) {
            projectAry.addAll(projectNamespace.getData());
        }
        if (federationProjectNamespace.getData() != null && !federationProjectNamespace.getData().isEmpty()) {
            projectAry.addAll(federationProjectNamespace.getData());
        }
        return projectAry;
    }


    /**
     * 查询当前用户所有分区
     * @param key
     * @return
     */
    public List<Namespace> listUserNamespace(String key) {
        List<ProjectDto> list = list(null);
        List<Namespace> namespaceList = new ArrayList<>();
        list.forEach(projectDto -> namespaceList.addAll(getNamespace(projectDto.getProjectId())));
        return namespaceList;
    }

    @Override
    public List<ProjectDto> list(String key) {
        List<ProjectDTO> projectDTOS = listAllTenantProject(ZeusCurrentUser.getCaasToken());
        List<ProjectDto> projects = new ArrayList<>();
        projectDTOS.forEach(projectDTO -> {
            ProjectDto project = new ProjectDto();
            project.setName(projectDTO.getProjectName());
            project.setAliasName(projectDTO.getProjectAliasName() + "（所属租户:" + projectDTO.getTenantAliasName() + "）");
            project.setNamespaceCount(projectDTO.getNamespaceCount());
            project.setUserDtoList(projectDTO.getUserDtos());
            project.setProjectId(projectDTO.getProjectId());
            project.setDescription(projectDTO.getDescription());
            project.setCreateTime(projectDTO.getCreateTime());
            project.setMemberCount(projectDTO.getMemberCount());
            project.setNamespaceCount(projectDTO.getNamespaceCount());
            projectTenantCache.put(project.getProjectId(), projectDTO.getTenantId());
            if (!StringUtils.isEmpty(key)) {
                if (project.getName().contains(key) || project.getAliasName().contains(key)) {
                    projects.add(project);
                }
            } else {
                projects.add(project);
            }
        });
        return projects;
    }

    @Override
    public List<Namespace> getNamespace(String projectId) {
        String tenantId = projectTenantCache.get(projectId);
        if (StringUtils.isEmpty(tenantId)) {
            listAllTenantProject(ZeusCurrentUser.getCaasToken());
            tenantId = projectTenantCache.get(projectId);
        }
        JSONArray projectNamespace = listProjectNamespace( tenantId, projectId);
        List<Namespace> namespaces = convertProjectNamespace(projectNamespace, projectId);
        Map<String, String> clusterMap = clusterService.listClusters().stream().
                collect(Collectors.toMap(MiddlewareClusterDTO::getId, MiddlewareClusterDTO::getNickname));
        namespaces.forEach(namespace -> {
            namespace.setClusterAliasName(clusterMap.get(namespace.getClusterId()));
        });
        return namespaces;
    }

    @Override
    public List<UserDto> getUser(String projectId, Boolean allocatable) { ;
        String tenantId = projectTenantCache.get(projectId);
        if (StringUtils.isEmpty(tenantId)) {
            listAllTenantProject(ZeusCurrentUser.getCaasToken());
            tenantId = projectTenantCache.get(projectId);
        }
        CaasResult<JSONObject> projectMember = projectServiceClient.getProjectMember(ZeusCurrentUser.getCaasToken(), tenantId, projectId);
        return convertProjectMember(projectMember.getJSONArray("userDataList"), projectId);
    }

    @Override
    public List<ProjectDto> getMiddlewareCount(String projectId) {
        List<Namespace> namespaceList;
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(projectId)) {
            namespaceList = getNamespace(projectId);
        } else {
            namespaceList = listUserNamespace(null);
        }
        // 分区为空
        if (CollectionUtils.isEmpty(namespaceList)) {
            return Collections.emptyList();
        }
        Set<String> clusterIdList =
                namespaceList.stream().map(Namespace::getClusterId).collect(Collectors.toSet());
        Map<String, List<MiddlewareCR>> middlewareCRListMap = new HashMap<>();
        for (String clusterId : clusterIdList) {
            List<MiddlewareCR> middlewareCRList = middlewareCRService.listCR(clusterId, null, null);
            middlewareCRListMap.put(clusterId, middlewareCRList);
        }
        Map<String, List<Namespace>> beanProjectNamespaceListMap =
                namespaceList.stream().collect(Collectors.groupingBy(Namespace::getProjectId));
        List<ProjectDto> projectDtoList = new ArrayList<>();
        for (String key : beanProjectNamespaceListMap.keySet()) {
            ProjectDto projectDto = new ProjectDto();
            projectDto.setProjectId(key);
            int count = 0;
            for (String clusterId : middlewareCRListMap.keySet()) {
                for (MiddlewareCR middlewareCr : middlewareCRListMap.get(clusterId)) {
                    if (beanProjectNamespaceListMap.get(key).stream()
                            .anyMatch(beanProjectNamespace -> beanProjectNamespace.getName()
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

}
