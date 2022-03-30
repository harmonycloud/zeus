package com.harmonycloud.zeus.service.user;

import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.caas.common.model.user.ProjectDto;
import com.harmonycloud.caas.common.model.user.UserDto;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/3/24 9:25 上午
 */
public interface ProjectService {

    /**
     * 新建项目
     * @param projectDto 项目对象
     *
     */
    void add(ProjectDto projectDto);

    /**
     * 查询项目列表
     * @return List<ProjectDto>
     */
    List<ProjectDto> list();

    /**
     * 查询项目下分区
     * @param projectId 项目id
     * @return List<Namespace>
     */
    List<Namespace> getNamespace(String projectId);

    /**
     * 查询项目下用户
     * @param projectId 项目id
     * @return List<UserDto>
     */
    List<UserDto> getUser(String projectId);

    /**
     * 项目绑定用户
     * @param projectDto 项目对象
     */
    void bindUser(ProjectDto projectDto);

    /**
     * 更新项目下用户角色
     * @param projectId 项目id
     * @param userDto   用户对象
     */
    void updateUserRole(String projectId, UserDto userDto);

    /**
     * 解绑项目下用户
     * @param projectId 项目id
     * @param username  用户名
     */
    void unbindUser(String projectId, String username);

    /**
     * 删除项目
     * @param projectId 项目id
     *
     */
    void delete(String projectId);

    /**
     * 更新项目
     * @param projectDto 项目对象
     *
     */
    void update(ProjectDto projectDto);

    /**
     * 项目绑定分区
     * @param namespace  分区对象
     */
    void bindNamespace(Namespace namespace);

    /**
     * 项目解绑分区
     * @param projectId 项目id
     * @param namespace 分区名称
     */
    void unBindNamespace(String projectId, String namespace);

}
