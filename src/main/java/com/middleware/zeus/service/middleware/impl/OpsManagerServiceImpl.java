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

import javax.annotation.PostConstruct;
import java.util.*;
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
    protected static final Map<String, List<String>> ID_MAP = new ConcurrentHashMap<>();

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
    public List<String> getMappingId(String id) {
        if (ID_MAP.isEmpty()) {
            List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
            for (MiddlewareClusterDTO cluster : clusterList) {
                try {
                    refresh(cluster.getId());
                } catch (Exception ignored) {
                }

            }
        }
        if (ID_MAP.containsKey(id)) {
            return ID_MAP.get(id);
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
                    ID_MAP.put(organizationDto.getOrganId(), Collections.singletonList(mongodbOrgDo.getId()));
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
            for (MongodbProjectDo mongodbProjectDo : mongodbProjectDoList) {
                // 判断名称开头是否相同，并判断组织是否匹配
                if (mongodbProjectDo.getName().startsWith(projectDto.getName() + "@")
                        && this.getMappingId(projectDto.getOrganId()).get(0).equals(mongodbProjectDo.getOrgId())) {
                    // 若匹配，则记录映射
                    if (ID_MAP.containsKey(projectDto.getProjectId())) {
                        ID_MAP.get(projectDto.getProjectId()).add(mongodbProjectDo.getId());
                    } else {
                        ID_MAP.put(projectDto.getProjectId(), Collections.singletonList(mongodbProjectDo.getId()));
                    }
                    break;
                }
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
                try {
                    createUser(cluster.getId(), userDto);
                } catch (Exception e) {
                    log.error("集群: {}, 创建ops manager用户失败", cluster.getId(), e);
                }
            }
            return;
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
        // 多集群遍历
        if (clusterId == null) {
            List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
            for (MiddlewareClusterDTO cluster : clusterList) {
                try {
                    deleteUser(cluster.getId(), username);
                } catch (Exception e){
                    log.error("集群: {}, 删除ops manager用户失败", cluster.getId(), e);

                }
            }
            return;
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // 根据用户名称获取用户id
        MongodbUserDo mongodbUserDo = mongodbClientWrapper.getUser(clusterId, map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), username);
        if (mongodbUserDo == null){
            return;
        }
        // 删除用户
        mongodbClientWrapper.deleteUser(clusterId, map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), mongodbUserDo.getId());
    }

    @Override
    public void createOrgan(String clusterId, String organId, String organName) {
        // 多集群遍历
        if (clusterId == null) {
            List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
            for (MiddlewareClusterDTO cluster : clusterList) {
                try {
                    createOrgan(cluster.getId(), organId, organName);
                } catch (Exception e) {
                    log.error("集群: {}, 创建ops manager组织失败", cluster.getId(), e);
                }
            }
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
                ID_MAP.put(organId, Collections.singletonList(mongodbOrgDo1.getId()));
                break;
            }
        }
    }

    @Override
    public void deleteOrgan(String clusterId, String organId) {
        // 多集群遍历
        if (clusterId == null) {
            List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
            for (MiddlewareClusterDTO cluster : clusterList) {
                try {
                    deleteOrgan(cluster.getId(), organId);
                } catch (Exception e) {
                    log.error("集群: {}, 删除ops manager组织失败", cluster.getId(), e);
                }
            }
            return;
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // 删除组织
        mongodbClientWrapper.deleteOrg(clusterId, map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), this.getMappingId(organId).get(0));
        // 移除缓存
        ID_MAP.remove(organId);
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
                try {
                    updateOrgan(cluster.getId(), organId, organName);
                } catch (Exception e) {
                    log.error("集群: {}, 更新ops manager组织失败", cluster.getId(), e);
                }
            }
            return;
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // 初始化数据结构
        MongodbOrgDo mongodbOrgDo = new MongodbOrgDo(Protocol.HTTP.getValue().toLowerCase(), path, port,
            map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), this.getMappingId(organId).get(0), organName);

        mongodbClientWrapper.updateOrgan(clusterId, mongodbOrgDo);
    }

    @Override
    public List<MongodbUserDo> listOrganUser(String clusterId, String organId) {
        if(StringUtils.isEmpty(clusterId)){
            return null;
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // 查询组织下用户
        return mongodbClientWrapper.listOrganUser(clusterId, map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), this.getMappingId(organId).get(0));
    }

    @Override
    public void refreshOrganUser(String clusterId, String organId, List<UserDto> userDtoList) {
        // 获取组织下用户
        // List<UserDto> userDtoList = organizationService.listOrganUser(organId, false);

        if (clusterId == null) {
            List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
            for (MiddlewareClusterDTO cluster : clusterList) {
                try {
                    refreshOrganUser(cluster.getId(), organId, userDtoList);
                } catch (Exception e) {
                    log.error("集群: {}, 刷新ops manager组织用户失败", cluster.getId(), e);
                    log.debug("集群: {}, 刷新ops manager组织用户失败", cluster.getId(), e);
                }
            }
            return;
        }

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
                this.allocateOrganUser(clusterId, organId, userDto.getUserName(), userDto.getRoleId());
            }
        }
    }

    @Override
    public void allocateOrganUser(String clusterId, String organId, String username, Integer roleId) {
        // 多集群遍历
        if (clusterId == null) {
            List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
            for (MiddlewareClusterDTO cluster : clusterList) {
                try {
                    allocateOrganUser(cluster.getId(), organId, username, roleId);
                } catch (Exception e) {
                    log.error("集群: {}, 分配ops manager组织用户失败", cluster.getId());
                    log.debug("集群: {}, 分配ops manager组织用户失败", cluster.getId(), e);
                }
            }
            return;
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

        // 初始化数据结构
        MongodbUserDo mongodbUserDo = new MongodbUserDo(Protocol.HTTP.getValue().toLowerCase(), path, port,
            map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), null, username, null, null, null, null, List.of(roleName));
        mongodbUserDo.setOrganizationId(this.getMappingId(organId).get(0));

        mongodbClientWrapper.allocateUserToOrgan(clusterId, mongodbUserDo);
    }

    @Override
    public void createProject(String clusterId, String orgId, String projectId, String projectName) {
        // 多集群遍历
        if (clusterId == null) {
            List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
            for (MiddlewareClusterDTO cluster : clusterList) {
                try {
                    createProject(cluster.getId(), orgId, projectId, projectName);
                } catch (Exception e) {
                    log.error("集群: {}, 创建ops manager项目失败", cluster.getId(), e);
                }
            }
            return;
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // 初始化数据结构
        MongodbProjectDo mongodbProjectDo = new MongodbProjectDo(Protocol.HTTP.getValue().toLowerCase(), path, port,
            map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), null, this.getMappingId(orgId).get(0), projectName);

        mongodbClientWrapper.createProject(clusterId, mongodbProjectDo);

        // 重新查询项目列表，记录项目id映射
        List<MongodbProjectDo> mongodbProjectDoList = this.listProjects(clusterId);
        for (MongodbProjectDo mongodbProjectDo1 : mongodbProjectDoList) {
            if (projectName.equals(mongodbProjectDo1.getName())) {
                ID_MAP.put(projectId, Collections.singletonList(mongodbProjectDo1.getId()));
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
                try {
                    updateProject(cluster.getId(), projectId, projectName);
                } catch (Exception e) {
                    log.error("集群: {}, 更新ops manager项目失败", cluster.getId(), e);
                }
            }
            return;
        }

        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // 获取项目匹配的所有id
        List<String> idList = this.getMappingId(projectId);

        // 初始化数据结构
        MongodbProjectDo mongodbProjectDo = new MongodbProjectDo(Protocol.HTTP.getValue().toLowerCase(), path, port,
                map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), null, null, null);
        // 获取所有需要更新的项目列表
        List<MongodbProjectDo> mongodbProjectDoList = this.listProjects(clusterId);
        // 遍历项目列表，更新项目名称
        this.listProjects(clusterId).forEach(mpd -> {
            if (idList.contains(mpd.getId())) {
                // 设置匹配上的项目的id和修改后的名称
                mongodbProjectDo.setId(mpd.getId());
                mongodbProjectDo.setName(projectName + "@" + mpd.getName().split("@")[1]);
                // 调用更新接口
                mongodbClientWrapper.updateProject(clusterId, mongodbProjectDo);
            }
        });
    }

    @Override
    public List<MongodbUserDo> listProjectUser(String clusterId, String projectId) {
        if (StringUtils.isEmpty(clusterId)) {
            return null;
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // 查询项目下用户
        return mongodbClientWrapper.listProjectUser(clusterId, map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), null,
            this.getMappingId(projectId).get(0));
    }

    @Override
    public void refreshProjectUser(String clusterId, String organId, String projectId, List<UserDto> userDtoList) {
        if (clusterId == null) {
            List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
            for (MiddlewareClusterDTO cluster : clusterList) {
                try {
                    refreshProjectUser(cluster.getId(), organId, projectId, userDtoList);
                } catch (Exception e) {
                    log.error("集群: {}, 刷新ops manager项目用户失败", cluster.getId());
                    log.debug("集群: {}, 刷新ops manager项目用户失败", cluster.getId(), e);
                }
            }
            return;
        }

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
                this.allocateProjectUser(clusterId, projectId, userDto.getUserName(), userDto.getRoleId());
            }
        }
    }

    @Override
    public void allocateProjectUser(String clusterId, String projectId, String username, Integer roleId) {
        // 多集群遍历
        if (clusterId == null) {
            List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
            for (MiddlewareClusterDTO cluster : clusterList) {
                try {
                    allocateProjectUser(cluster.getId(), projectId, username, roleId);
                } catch (Exception e) {
                    log.error("集群: {}, 分配ops manager项目用户失败", cluster.getId());
                    log.debug("集群: {}, 分配ops manager项目用户失败", cluster.getId(), e);
                }
            }
            return;
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
                roleName = "GROUP_DATA_ACCESS_READ_ONLY";
                break;
        }

        // 初始化数据结构
        MongodbUserDo mongodbUserDo = new MongodbUserDo(Protocol.HTTP.getValue().toLowerCase(), path, port,
            map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), null, username, null, null, null, null, List.of(roleName));

        // 获取匹配的项目列表
        this.listProjects(clusterId).forEach(mongodbProjectDo -> {
            if (mongodbProjectDo.getName().equals(projectId)) {
                mongodbUserDo.setProjectId(mongodbProjectDo.getId());
            }

        });

        mongodbClientWrapper.allocateUserToProject(clusterId, mongodbUserDo);
    }

    @Override
    public void deleteProject(String clusterId, String projectName) {
        // 多集群遍历
        if (clusterId == null) {
            List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
            for (MiddlewareClusterDTO cluster : clusterList) {
                try {
                    deleteProject(cluster.getId(), projectName);
                } catch (Exception e) {
                    log.error("集群: {}, 删除ops manager项目失败", cluster.getId(), e);
                }
            }
            return;
        }
        // 查询用户认证信息
        Map<String, String> map = getPublicAndPrivateKey(clusterId);
        // 查询名称匹配的项目，并调用删除接口
        this.listProjects(clusterId).forEach(mongodbProjectDo -> {
            if (mongodbProjectDo.getName().equals(projectName)) {
                // 删除项目
                mongodbClientWrapper.deleteProject(clusterId, map.get(PUBLIC_KEY), map.get(PRIVATE_KEY), mongodbProjectDo.getId());
            }
        });
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
