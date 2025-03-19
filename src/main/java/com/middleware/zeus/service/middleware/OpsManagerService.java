package com.middleware.zeus.service.middleware;

import com.middleware.zeus.common.model.middleware.mongodb.MongodbOrgDo;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbProjectDo;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbUserDo;
import com.middleware.zeus.common.model.user.UserDto;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2025/3/3 2:49 PM
 */
public interface OpsManagerService {

    /**
     * 刷新ID_MAP
     */
    void clearIdMap();

    /**
     * 获取组织/项目id
     * @param id id
     * @return String
     */
    List<String> getMappingId(String id);

    /**
     * 刷新组织项目用户信息
     * @param clusterId
     */
    void refresh(String clusterId);

    /**
     * 创建用户
     */
    void createUser(String clusterId, UserDto userDto);


    /**
     * 删除用户
     */
    void deleteUser(String clusterId, String username);

    /**
     * 创建组织
     * @param clusterId 集群id
     * @param organId 组织id
     * @param organName 组织名称
     */
    void createOrgan(String clusterId, String organId, String organName);

    /**
     * 创建组织
     * @param clusterId 集群id
     * @param organId 组织id
     */
    void deleteOrgan(String clusterId, String organId);

    /**
     * 查询组织列表
     * @param clusterId 集群id
     * @return List<MongodbOrgDo>
     */
    List<MongodbOrgDo> listOrgans(String clusterId);

    /**
     * 更新组织
     * @param clusterId 集群id
     * @param organId 组织id
     * @param organName 组织名称
     */
    void updateOrgan(String clusterId, String organId, String organName);

    /**
     * 查询组织下用户
     * @param clusterId 集群id
     * @param organId 组织id
     */
    List<MongodbUserDo> listOrganUser(String clusterId, String organId);

    /**
     * 更新组织下用户
     * @param clusterId 集群id
     * @param organId 组织id
     * @param userDtoList 用户列表
     */
    void refreshOrganUser(String clusterId, String organId, List<UserDto> userDtoList);

    /**
     * 分配组织下用户
     * @param clusterId 集群id
     * @param organId 组织id
     */
    void allocateOrganUser(String clusterId, String organId, String username, Integer roleId);

    /**
     * 创建项目
     * @param clusterId 集群id
     * @param orgId 组织id
     * @param projectId 项目id
     * @param projectName 项目名称
     */
    void createProject(String clusterId, String orgId, String projectId, String projectName);

    /**
     * 查询项目列表
     * @param clusterId 集群id
     * @return List<MongodbOrgDo>
     */
    List<MongodbProjectDo> listProjects(String clusterId);

    /**
     * 更新项目
     * @param clusterId 集群id
     * @param organId 组织id
     * @param projectName 项目名称
     */
    void updateProject(String clusterId, String organId, String projectName);

    /**
     * 查询项目下用户
     * @param clusterId 集群id
     * @param projectId 项目id
     */
    Map<String, List<MongodbUserDo>> listProjectUser(String clusterId, String projectId);

    /**
     * 更新项目下用户
     * @param clusterId 集群id
     * @param organId 组织id
     * @param projectId 项目id
     * @param userDtoList 用户列表
     */
    void refreshProjectUser(String clusterId, String organId, String projectId, List<UserDto> userDtoList);

    /**
     * 分配项目下用户
     * @param clusterId 集群id
     * @param projectId 项目id
     * @param username 项目名称
     * @param roleId 角色id
     * @param id ops manager中项目id
     */
    void allocateProjectUser(String clusterId, String projectId, String username, Integer roleId, String id);

    /**
     * 删除项目
     * @param clusterId 集群id
     * @param projectName 项目名称
     */
    void deleteProject(String clusterId, String projectName);


}
