package com.harmonycloud.zeus.service.user.skyviewimpl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.CaasResult;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.harmonycloud.caas.common.model.ClusterDTO;
import com.harmonycloud.caas.common.model.ProjectDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareResourceInfo;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.caas.common.model.middleware.ProjectMiddlewareResourceInfo;
import com.harmonycloud.caas.common.model.user.ProjectDto;
import com.harmonycloud.caas.common.model.user.RoleDto;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.zeus.bean.BeanClusterMiddlewareInfo;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.bean.user.BeanUserRole;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCR;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.middleware.ClusterMiddlewareInfoService;
import com.harmonycloud.zeus.service.user.RoleService;
import com.harmonycloud.zeus.service.user.UserRoleService;
import com.harmonycloud.zeus.service.user.UserService;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.user.UserConstant.USERNAME;

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
    @Autowired
    private ClusterMiddlewareInfoService clusterMiddlewareInfoService;
    @Autowired
    private UserService userService;
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
        ExecutorService executorService = Executors.newFixedThreadPool(tenants.size());
        CountDownLatch latch = new CountDownLatch(tenants.size());
        for (Object tenant : tenants) {
            executorService.submit(()->{
                try {
                    JSONObject jsonTenant = (JSONObject) tenant;
                    log.info("提交查询租户信息:{}", jsonTenant.getString("tenantId"));
                    CaasResult<JSONArray> projectResult = projectServiceClient.getTenantProject(caastoken, jsonTenant.getString("tenantId"));
                    if (Boolean.TRUE.equals(projectResult.getSuccess())) {
                        JSONArray projectList = projectResult.getData();
                        projects.addAll(convertProject(projectList, jsonTenant.getString("tenantName"), jsonTenant.getString("aliasName"), caastoken));
                    }
                } catch (Exception e) {
                    log.error("查询租户项目出错了", e);
                } finally {
                    log.info("租户查询完成");
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
            executorService.shutdown();
        } catch (InterruptedException e) {
            log.error("查询用户所有项目出错了", e);
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
        ExecutorService executorService = Executors.newFixedThreadPool(projects.size());
        CountDownLatch latch = new CountDownLatch(projects.size());
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
            executorService.submit(() -> {
                try {
                    log.debug("开始查询项目{}的成员", projectDTO.getProjectName());
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
                } catch (Exception e) {
                    log.error("查询项目{}成员出错了", projectDTO.getProjectName(), e);
                } finally {
                    log.info("项目{}成员查询完成", projectDTO.getProjectName());
                    latch.countDown();
                }
            });
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("查询项目列表出错了",e);
        }finally {
            executorService.shutdown();
        }
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
                    ns.setClusterId(clusterService.convertToZeusClusterId(jsonCluster.getString("id")));
                } else {
                    ns.setClusterId(clusterService.convertToZeusClusterId(jsonNamespace.getString("clusterId")));
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
            userDto.setId(jsonUser.getInteger("id"));
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
                    userDto.setRoleId(4);
                    userDto.setRoleName("普通用户");
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
     * @return
     */
    public List<Namespace> listUserNamespace() {
        List<ProjectDto> projectDtoList = list(null);
        List<Namespace> namespaceList = new ArrayList<>();
        projectDtoList.forEach(projectDto -> namespaceList.addAll(getNamespace(projectDto.getProjectId())));
        return namespaceList;
    }

    @Override
    public List<ProjectDto> list(String keyword) {
        userService.getUserDto(ZeusCurrentUser.getUserName(), null);
        List<ProjectDTO> projectDTOS = listAllTenantProject(ZeusCurrentUser.getCaasToken());
        List<ProjectDto> projects = new ArrayList<>();
        projectDTOS.forEach(projectDTO -> {
            ProjectDto project = new ProjectDto();
            project.setName(projectDTO.getProjectName());
            project.setAliasName(projectDTO.getProjectAliasName());
            project.setTenantAliasName(projectDTO.getTenantAliasName());
            project.setNamespaceCount(projectDTO.getNamespaceCount());
            project.setUserDtoList(projectDTO.getUserDtos());
            project.setProjectId(projectDTO.getProjectId());
            project.setDescription(projectDTO.getDescription());
            project.setCreateTime(projectDTO.getCreateTime());
            project.setMemberCount(projectDTO.getMemberCount());
            project.setNamespaceCount(projectDTO.getNamespaceCount());
            BeanUserRole beanUserRole = userRoleService.get(ZeusCurrentUser.getUserName(), projectDTO.getProjectId());
            projectTenantCache.put(project.getProjectId(), projectDTO.getTenantId());
            if (ZeusCurrentUser.isAdmin()) {
                projects.add(project);
            } else if (beanUserRole != null) {
                RoleDto roleDto = roleService.get(beanUserRole.getRoleId());
                project.setRoleId(beanUserRole.getRoleId());
                project.setRoleName(roleDto.getName());
                projects.add(project);
            }else {
                log.info("用户{}在项目{}下没有角色", ZeusCurrentUser.getUserName(), projectDTO.getProjectName());
            }
        });
        if (!StringUtils.isEmpty(keyword)) {
            return projects.stream().filter(projectDto -> projectDto.getName().contains(keyword)).collect(Collectors.toList());
        }
        return projects;
    }

    @Override
    public List<Namespace> getNamespace(String projectId) {
        String tenantId = projectTenantCache.get(projectId);
        if (StringUtils.isEmpty(tenantId)) {
            listAllTenantProject(ZeusCurrentUser.getCaasToken());
            tenantId = projectTenantCache.get(projectId);
        }
        JSONArray projectNamespace = listProjectNamespace(tenantId, projectId);
        List<Namespace> namespaces = convertProjectNamespace(projectNamespace, projectId);
        namespaces.forEach(namespace -> {
            ClusterDTO clusterDTO = clusterService.findBySkyviewClusterId(clusterService.convertToSkyviewClusterId(namespace.getClusterId()));
            if (clusterDTO != null) {
                namespace.setClusterAliasName(clusterDTO.getAliasName());
            }
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
            namespaceList = listUserNamespace();
        }
        // 分区为空
        if (CollectionUtils.isEmpty(namespaceList)) {
            return Collections.emptyList();
        }
        Set<String> clusterIdList =
                namespaceList.stream().map(Namespace::getClusterId).collect(Collectors.toSet());
        Map<String, List<MiddlewareCR>> middlewareCRListMap = new HashMap<>();
        for (String clusterId : clusterIdList) {
            List<MiddlewareCR> middlewareCRList = null;
            try {
                middlewareCRList = middlewareCRService.listCR(clusterId, null, null);
                middlewareCRListMap.put(clusterId, middlewareCRList);
            } catch (Exception e) {
                log.error("统计集群{}的服务数量出错了", clusterId, e);
            }
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

    @Override
    public List<ProjectMiddlewareResourceInfo> middlewareResource(String projectId) throws Exception {
        List<Namespace> namespaceList = getNamespace(projectId);
        // 获取集群
        Set<String> clusterIdSet = new HashSet<>();
        namespaceList.forEach(beanProjectNamespace -> {
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
        if (!userDto.getIsAdmin() && userDto.getUserRoleList().stream().anyMatch(userRole -> userRole.getProjectId().equals(projectId))){
            power.putAll(userDto.getUserRoleList().stream().filter(userRole -> userRole.getProjectId().equals(projectId))
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
        all = all.stream().filter(middlewareResourceInfo -> namespaceList.stream().anyMatch(
                namespace -> namespace.getName().equals(middlewareResourceInfo.getNamespace())))
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
        for (String mwType : mwTypeSet) {
            ProjectMiddlewareResourceInfo projectMiddlewareResourceInfo = new ProjectMiddlewareResourceInfo()
                    .setType(mwType).setAliasName(MiddlewareOfficialNameEnum.findByChartName(mwType))
                    .setMiddlewareResourceInfoList(map.getOrDefault(mwType, null))
                    .setImagePath(middlewareImagePathMap.getOrDefault(mwType, null));
            infoList.add(projectMiddlewareResourceInfo);
        }
        return infoList;
    }

}
