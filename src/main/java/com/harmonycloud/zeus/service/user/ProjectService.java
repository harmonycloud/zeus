package com.harmonycloud.zeus.service.user;

import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareResourceInfo;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.caas.common.model.middleware.ProjectMiddlewareResourceInfo;
import com.harmonycloud.caas.common.model.user.ProjectDto;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.zeus.bean.user.BeanProject;
import com.harmonycloud.zeus.bean.user.BeanProjectNamespace;

import java.util.List;
import java.util.Map;

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
     * 保存项目信息
     * @param beanProject
     */
    void add(BeanProject beanProject);

    /**
     * 查询项目列表
     * @return List<ProjectDto>
     */
    List<ProjectDto> list(String keyword);

    /**
     * 查询指定集群项目下分区
     * @param projectId 项目id
     * @param clusterId 集群id
     * @return List<Namespace>
     */
    List<Namespace> getNamespace(String projectId, String clusterId, Boolean withQuota);

    /**
     * 查询项目下分区
     * @param projectId
     * @return
     */
    List<Namespace> getNamespace(String projectId);

    /**
     * 查询项目下分区
     * @return List<Namespace>
     */
    List<MiddlewareClusterDTO> getAllocatableNamespace();

    /**
     * 查询项目下用户
     * @param projectId 项目id
     * @return List<UserDto>
     */
    List<UserDto> getUser(String projectId, Boolean allocatable);

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
     * 更新项目
     * @param beanProject
     */
    void update(BeanProject beanProject);

    /**
     * 项目绑定分区
     * @param namespace  分区对象
     */
    void bindNamespace(Namespace namespace);

    /**
     * 项目绑定分区
     * @param namespaceList
     */
    void bindNamespace(List<Namespace> namespaceList);

    /**
     * 项目解绑分区
     * @param projectId 项目id
     * @param clusterId 集群id
     * @param namespace 分区名称
     * @param checkExist 中间件存在校验
     */
    void unBindNamespace(String projectId, String clusterId, String namespace, Boolean checkExist);

    /**
     * 项目解绑分区
     * @param projectId 项目id
     * @param clusterId 集群id
     * @param namespace 分区名称
     */
    void unBindNamespace(String projectId, String clusterId, String namespace);

    /**
     * 获取项目下中间件资源
     * @param projectId 项目id
     */
    List<ProjectMiddlewareResourceInfo> middlewareResource(String projectId) throws Exception;

    /**
     * 查询项目列表
     * @return List<ProjectDto>
     */
    List<String> getClusters(String projectId);

    /**
     * 查询项目列表
     * @return List<ProjectDto>
     */
    List<ProjectDto> getMiddlewareCount(String projectId);


    /**
     * 通过分区查询项目
     * @param namespace 分区
     *
     * @return ProjectDto
     */
    ProjectDto findProjectByNamespace(String namespace);

    /**
     * 查询项目
     * @param projectId
     * @return
     */
    BeanProject get(String projectId);

}
