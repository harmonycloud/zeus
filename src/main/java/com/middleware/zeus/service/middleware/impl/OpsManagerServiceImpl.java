package com.middleware.zeus.service.middleware.impl;

import com.middleware.zeus.common.enums.Protocol;
import com.middleware.zeus.common.model.Secret;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbOrgDo;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbProjectDo;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbRoleDo;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbUserDo;
import com.middleware.zeus.common.model.user.OrganizationDto;
import com.middleware.zeus.common.model.user.ProjectDto;
import com.middleware.zeus.common.model.user.UserDto;
import com.middleware.zeus.integration.dashboard.MongodbClientWrapper;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.k8s.SecretService;
import com.middleware.zeus.service.middleware.OpsManagerService;
import com.middleware.zeus.service.user.OrganizationService;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.service.user.UserService;
import com.middleware.zeus.util.encrypt.Base64Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.middleware.zeus.common.constants.CommonConstant.SLASH;
import static com.middleware.zeus.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_OPERATOR;

/**
 * @author xutianhong
 * @Date 2025/3/3 2:49 PM
 */
@Slf4j
@Service
public class OpsManagerServiceImpl implements OpsManagerService {

    // 缓存公私钥
    protected static final Map<String, String> KEY_CACHE = new ConcurrentHashMap<>();

    // 缓存组织/项目id映射
    protected static final Map<String, String> ID_MAP = new ConcurrentHashMap<>();

    public static final String PUBLIC_KEY = "publicKey";
    public static final String PRIVATE_KEY = "privateKey";



    @Value("${system.opsManager.path:mongodb-enterprise-operator-om-svc.middleware-operator}")
    private String path;
    @Value("${system.opsManager.port:8080}")
    private String port;

    @Autowired
    private MongodbClientWrapper mongodbClientWrapper;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private SecretService secretService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserService userService;

    @Override
    public String getOrgId(String organId) {
        if (ID_MAP.containsKey(organId)) {
            return ID_MAP.get(organId);
        }
        return null;
    }

    @Override
    public void refresh(String clusterId) {
        // 获取平台组织列表
        List<OrganizationDto> organizationDtoList = organizationService.list(null);
        // 获取平台项目列表
        List<ProjectDto> projectDtoList = projectService.list(null);

        //获取opsManager组织列表
        List<MongodbOrgDo> mongodbOrgDoList = this.listOrgans(clusterId);
        // 过滤已删除的组织
        mongodbOrgDoList.removeIf(MongodbOrgDo::getIsDeleted);
        // 获取opsManager项目列表
        List<MongodbProjectDo> mongodbProjectDoList = this.listProjects(clusterId);

        // 比对组织项目列表，更新opsManager组织项目列表
        for (OrganizationDto organizationDto : organizationDtoList) {
            boolean exist = false;
            for (MongodbOrgDo mongodbOrgDo : mongodbOrgDoList) {
                if (organizationDto.getName().equals(mongodbOrgDo.getName())) {
                    exist = true;
                    ID_MAP.put(organizationDto.getOrganId(), mongodbOrgDo.getId());
                    break;
                }
            }
            if (!exist) {
                MongodbOrgDo mongodbOrgDo = new MongodbOrgDo();
                mongodbOrgDo.setName(organizationDto.getName());
                this.createOrgan(clusterId, organizationDto.getOrganId(), organizationDto.getName());
            }
        }

        // 比对项目列表，更新opsManager项目列表
        for (ProjectDto projectDto : projectDtoList) {
            boolean exist = false;
            for (MongodbProjectDo mongodbProjectDo : mongodbProjectDoList) {
                if (projectDto.getName().equals(mongodbProjectDo.getName())) {
                    exist = true;
                    ID_MAP.put(projectDto.getProjectId(), mongodbProjectDo.getId());
                    break;
                }
            }
            if (!exist) {
                MongodbProjectDo mongodbProjectDo = new MongodbProjectDo();
                mongodbProjectDo.setName(projectDto.getName());
                this.createProject(clusterId, projectDto.getOrganId(), projectDto.getProjectId(), projectDto.getName());
            }
        }

        // 同步初始化用户(创建用户)
        // 获取平台用户列表
        List<UserDto> userDtoList = userService.list(null);
        // 循环调用创建接口，忽略已存在的异常
        for (UserDto userDto : userDtoList) {
            try {
                this.createUser(clusterId, userDto);
            } catch (Exception e){
                log.error("创建ops manager用户失败");
            }
        }
    }

    @Override
    public void createUser(String clusterId, UserDto userDto) {
        // 多集群遍历
        if (clusterId == null) {
            List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
            for (MiddlewareClusterDTO cluster : clusterList) {
                createUser(cluster.getId(), userDto);
            }
        }

        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);

        // 初始化数据结构
        // 名与姓暂时都采用用户名
        MongodbUserDo mongodbUserDo = new MongodbUserDo(Protocol.HTTP.getValue().toLowerCase(), path, port,
                map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), null, userDto.getUserName(), userDto.getEmail(),
                userDto.getUserName(), userDto.getUserName(), "zeus123.com", null);

        // todo 角色映射（是否需要？）
        mongodbClientWrapper.createUser(clusterId, mongodbUserDo);
    }

    @Override
    public void deleteUser(String clusterId, String username) {
        // todo
        // 根据用户名称获取用户id

    }

    @Override
    public void createOrgan(String clusterId, String organId, String organName) {
        if (StringUtils.isAnyEmpty(clusterId, organName)) {
            return;
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // 初始化数据结构
        MongodbOrgDo mongodbOrgDo = new MongodbOrgDo(Protocol.HTTP.getValue().toLowerCase(), path, port,
            map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), null, organName);
        // 创建组织
        mongodbClientWrapper.createOrg(clusterId, mongodbOrgDo);

        // 重新查询组织列表，记录组织id映射
        // todo 设置缓存的位置需要调整
        List<MongodbOrgDo> mongodbOrgDoList = this.listOrgans(clusterId);
        for (MongodbOrgDo mongodbOrgDo1 : mongodbOrgDoList) {
            if (organName.equals(mongodbOrgDo1.getName())) {
                ID_MAP.put(organId, mongodbOrgDo1.getId());
                break;
            }
        }
    }

    @Override
    public List<MongodbOrgDo> listOrgans(String clusterId) {
        if (StringUtils.isEmpty(clusterId)) {
            return null;
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // 查询组织列表
        return mongodbClientWrapper.listOrgans(clusterId, map.get(PUBLIC_KEY), map.get(PRIVATE_KEY));
    }

    @Override
    public void updateOrgan(String clusterId, String organId, String organName) {
        // 多集群遍历
        if (clusterId == null) {
            List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
            for (MiddlewareClusterDTO cluster : clusterList) {
                updateOrgan(cluster.getId(), organId, organName);
            }
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // 初始化数据结构
        MongodbOrgDo mongodbOrgDo = new MongodbOrgDo(Protocol.HTTP.getValue().toLowerCase(), path, port,
            map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), ID_MAP.get(organId), organName);

        mongodbClientWrapper.updateOrgan(clusterId, mongodbOrgDo);
    }

    @Override
    public List<MongodbUserDo> listOrganUser(String clusterId, String organId) {
        if(StringUtils.isEmpty(clusterId)){
            return null;
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // todo 修改接口
        return mongodbClientWrapper.listOrganUser(clusterId, map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), ID_MAP.get(organId), null);
    }

    @Override
    public void refreshOrganUser(String clusterId, String organId) {
        // todo 考虑什么触发刷新
        // 获取组织下用户
        List<UserDto> userDtoList = organizationService.listOrganUser(organId, false);

        // 获取ops manager中的组织下的用户
        List<MongodbUserDo> mongodbUserDoList = this.listOrganUser(clusterId, organId);

        // 比较用户，更新ops manager中的组织下的用户
        for (UserDto userDto : userDtoList) {
            boolean exist = false;
            for (MongodbUserDo mongodbUserDo : mongodbUserDoList) {
                if (userDto.getUserName().equals(mongodbUserDo.getUsername())) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                this.createUser(clusterId, userDto);
            }
        }
    }

    @Override
    public void allocateOrganUser(String clusterId, String organId, String username, Integer roleId) {
        // 多集群遍历
        if (clusterId == null) {
            List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
            for (MiddlewareClusterDTO cluster : clusterList) {
                allocateOrganUser(cluster.getId(), organId, username, roleId);
            }
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // 根据roleId 映射roleName
        String roleName;
        if (roleId == null) {
            roleName = "ORG_MEMBER";
        } else {
            roleName = "ORG_OWNER";
        }

        MongodbRoleDo mongodbRoleDo = new MongodbRoleDo();
        mongodbRoleDo.setRoleName(roleName);

        // 初始化数据结构
        MongodbUserDo mongodbUserDo = new MongodbUserDo(Protocol.HTTP.getValue().toLowerCase(), path, port,
            map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), null, username, null, null, null, null, List.of(mongodbRoleDo));
        mongodbUserDo.setOrganizationId(ID_MAP.get(organId));

        mongodbClientWrapper.allocateUserToOrgan(clusterId, mongodbUserDo);
    }

    @Override
    public void createProject(String clusterId, String orgId, String projectId, String projectName) {
        if (StringUtils.isAnyEmpty(clusterId, projectName)) {
            return;
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // 初始化数据结构
        MongodbProjectDo mongodbProjectDo = new MongodbProjectDo(Protocol.HTTP.getValue().toLowerCase(), path, port,
            map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), null, ID_MAP.get(orgId), projectName);

        mongodbClientWrapper.createProject(clusterId, mongodbProjectDo);

        // 重新查询项目列表，记录项目id映射
        List<MongodbProjectDo> mongodbProjectDoList = this.listProjects(clusterId);
        for (MongodbProjectDo mongodbProjectDo1 : mongodbProjectDoList) {
            if (projectName.equals(mongodbProjectDo1.getName())) {
                ID_MAP.put(projectId, mongodbProjectDo1.getId());
                break;
            }
        }
    }

    @Override
    public List<MongodbProjectDo> listProjects(String clusterId) {
        if (StringUtils.isEmpty(clusterId)) {
            return null;
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        return mongodbClientWrapper.listAllProjects(clusterId, map.get(PUBLIC_KEY), map.get(PRIVATE_KEY));
    }

    @Override
    public void updateProject(String clusterId, String projectId, String projectName) {
        // 多集群遍历
        if (clusterId == null) {
            List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
            for (MiddlewareClusterDTO cluster : clusterList) {
                updateProject(cluster.getId(), projectId, projectName);
            }
        }

        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // 初始化数据结构
        MongodbProjectDo mongodbProjectDo = new MongodbProjectDo(Protocol.HTTP.getValue().toLowerCase(), path, port,
            map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), ID_MAP.get(projectId), null, projectName);

        mongodbClientWrapper.updateProject(mongodbProjectDo);
    }

    @Override
    public List<MongodbUserDo> listProjectUser(String clusterId, String projectId) {
        if (StringUtils.isEmpty(clusterId)) {
            return null;
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // todo 修改接口
        return mongodbClientWrapper.listProjectUser(clusterId, map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), null, ID_MAP.get(projectId));
    }

    @Override
    public void refreshProjectUser(String clusterId, String organId, String projectId) {
        // todo 考虑什么触发刷新
        // 获取项目下用户
        List<UserDto> userDtoList = projectService.getUser(organId, projectId, false);

        // 获取ops manager中的项目下的用户
        List<MongodbUserDo> mongodbUserDoList = this.listProjectUser(clusterId, projectId);

        // 比较用户，更新ops manager中的项目下的用户
        for (UserDto userDto : userDtoList) {
            boolean exist = false;
            for (MongodbUserDo mongodbUserDo : mongodbUserDoList) {
                if (userDto.getUserName().equals(mongodbUserDo.getUsername())) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                this.createUser(clusterId, userDto);
            }
        }
    }

    @Override
    public void allocateProjectUser(String clusterId, String projectId, String username, Integer roleId) {
        // 多集群遍历
        if (clusterId == null) {
            List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
            for (MiddlewareClusterDTO cluster : clusterList) {
                allocateProjectUser(cluster.getId(), projectId, username, roleId);
            }
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // 根据roleId 映射roleName
        String roleName;
        switch (roleId) {
            case 2:
                roleName = "GROUP_OWNER";
                break;
            case 3:
                roleName = "GROUP_DATA_ACCESS_ADMIN";
                break;
            default:
                roleName = "Project Data Access Read Only";
                break;
        }

        MongodbRoleDo mongodbRoleDo = new MongodbRoleDo();
        mongodbRoleDo.setRoleName(roleName);

        // 初始化数据结构
        MongodbUserDo mongodbUserDo = new MongodbUserDo(Protocol.HTTP.getValue().toLowerCase(), path, port,
            map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), null, username, null, null, null, null, List.of(mongodbRoleDo));
        mongodbUserDo.setProjectId(ID_MAP.get(projectId));

        mongodbClientWrapper.allocateUserToProject(clusterId, mongodbUserDo);
    }

    @Override
    public void deleteProject() {
        // todo 考虑是否需要删除组织和项目
    }

    private Map<String, String> getPublicAndPrivateKey(String clusterId) {
        // 确认是否已存在缓存
        if (KEY_CACHE.containsKey(clusterId)) {
            return Map.of(PUBLIC_KEY, KEY_CACHE.get(clusterId).split(SLASH)[0], PRIVATE_KEY,
                KEY_CACHE.get(clusterId).split(SLASH)[1]);
        }
        // 查询用户认证信息
        Secret secret = secretService.get(clusterId, MIDDLEWARE_OPERATOR,
            "middleware-operator-mongodb-enterprise-operator-om-admin-key");
        String publicKey = new String(Base64Utils.decode(secret.getData().get("publicKey")));
        String privateKey = new String(Base64Utils.decode(secret.getData().get("privateKey")));

        // 设置缓存
        KEY_CACHE.put(clusterId, publicKey + SLASH + privateKey);
        // 返回数据
        return Map.of(PUBLIC_KEY, publicKey, PRIVATE_KEY, privateKey);
    }
}
