package com.middleware.zeus.service.user;

import com.github.pagehelper.PageInfo;
import com.middleware.zeus.common.model.BackupServerDTO;
import com.middleware.zeus.common.model.MiddlewareResourceQueryDto;
import com.middleware.zeus.common.model.ResourceQuotaDo;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.common.model.middleware.MiddlewareResourceInfo;
import com.middleware.zeus.common.model.middleware.Namespace;
import com.middleware.zeus.common.model.middleware.ProjectMiddlewareResourceInfo;
import com.middleware.zeus.common.model.user.*;
import com.middleware.zeus.annotation.Skyview;
import com.middleware.zeus.bean.user.BeanProject;

import java.util.List;
import java.util.Set;

/**
 * @author xutianhong
 * @Date 2022/3/24 9:25 上午
 */
@Skyview
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
     * 查询项目
     * @param organId 组织id
     * @param projectId 项目id
     * @return
     */
    ProjectDto get(String organId, String projectId);

    /**
     * 查询项目列表
     * @return List<ProjectDto>
     */
    List<ProjectDto> list(String organId);

    /**
     * 查询项目列表
     * @return List<ProjectDto>
     */
    List<ProjectDto> list(String organId, String keyword);

    /**
     * 查询指定集群项目下分区
     * @param organId 组织id
     * @param projectId 项目id
     * @param clusterId 集群id
     * @return List<Namespace>
     */
    List<Namespace> getNamespace(String organId, String projectId, String clusterId, Boolean withQuota, Boolean withMiddleware);

    /**
     * 查询项目下分区
     * @param projectId
     * @return
     */
    List<Namespace> getNamespace(String organId, String projectId);

    /**
     * 查询分区所属项目
     * @return List<BeanProjectNamespace>
     */
    List<ProjectNamespaceDo> listNamespace(String clusterId);

    /**
     * 查询项目下分区
     * @return List<Namespace>
     */
    List<MiddlewareClusterDTO> getAllocatableNamespace();

    /**
     * 查询项目下用户
     * @param organId 组织id
     * @param projectId 项目id
     * @return List<UserDto>
     */
    List<UserDto> getUser(String organId, String projectId, Boolean allocatable);

    /**
     * 项目绑定用户
     * @param projectDto 项目对象
     */
    void bindUser(ProjectDto projectDto);

    /**
     * 更新项目下用户角色
     * @param organId 组织id
     * @param projectId 项目id
     * @param userDto   用户对象
     */
    void updateUserRole(String organId, String projectId, UserDto userDto);

    /**
     * 解绑项目下用户
     * @param organId
     * @param projectId 项目id
     * @param username  用户名
     */
    void unbindUser(String organId, String projectId, String username);

    /**
     * 删除项目
     * @param organId 组织id
     * @param projectId 项目id
     *
     */
    void delete(String organId, String projectId);

    /**
     * 更新项目
     * @param projectDto 项目对象
     *
     */
    void update(ProjectDto projectDto);

    /**
     * 项目下创建
     * @param namespace  分区对象
     */
    void addNamespace(Namespace namespace);

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
     * @param organId 组织id
     * @param projectId 项目id
     * @param clusterId 集群id
     * @param namespace 分区名称
     * @param checkExist 中间件存在校验
     */
    void unBindNamespace(String organId, String projectId, String clusterId, String namespace, Boolean checkExist);

    /**
     * 项目解绑分区
     * @param organId 组织id
     * @param projectId 项目id
     * @param clusterId 集群id
     * @param namespace 分区名称
     */
    void unBindNamespace(String organId, String projectId, String clusterId, String namespace);

    /**
     * 获取项目下中间件资源
     * @param organId 组织id
     * @param projectId 项目id
     *
     * @return List<ProjectMiddlewareResourceInfo>
     */
    PageInfo<MiddlewareResourceInfo> middlewareResource(String organId, String projectId, MiddlewareResourceQueryDto queryDto) throws Exception;

    /**
     * 获取项目下中间件资源
     * @param organId 组织id
     * @param projectId 项目id
     *
     * @return List<ProjectMiddlewareResourceInfo>
     */
    List<String> userMiddlewareType(String organId, String projectId);

    /**
     * 查询项目列表
     * @return List<ProjectDto>
     */
    Set<String> getRelationClusterIds(String organId, String projectId);

    /**
     * 查询项目列表
     * @param organId 组织id
     * @param projectId 项目id
     * @return List<ProjectDto>
     */
    List<ProjectDto> getMiddlewareCount(String organId, String projectId);

    /**
     * 资源分配
     * @param projectQuota 项目资源对象
     *
     */
    void allocateQuota(ProjectQuota projectQuota);

    /**
     * 获取项目存储信息
     * @param organId 组织id
     * @param projectId 项目id
     * @param clusterId 集群id
     * @param detail 是否包含使用情况
     *
     * @return  List<ResourceQuotaDo>
     */
    List<ResourceQuotaDo> getStorageQuota(String organId, String projectId, String clusterId, boolean detail);

    /**
     * 移除项目存储配额
     * @param organId 组织id
     * @param projectId 项目id
     * @param storageId 存储id
     * @param clusterId 集群id
     *
     */
    void removeStorageQuota(String organId, String projectId, String storageId, String clusterId);

    /**
     * 获取项目cpu memory信息
     * @param organId 组织id
     * @param projectId 项目id
     * @param detail 是否包含使用情况
     *
     * @return  List<ResourceQuotaDo>
     */
    List<ResourceQuotaDo> getCpuMemoryQuota(String organId, String projectId, boolean detail);

    /**
     * 移除项目cpu memory配额
     * @param organId 组织id
     * @param projectId 项目id
     * @param clusterId 集群id
     *
     */
    void removeCpuMemoryQuota(String organId, String projectId, String clusterId);

    /**
     * 获取项目备份服务器信息
     * @param organId 组织id
     * @param projectId 项目id
     * @param clusterId
     * @param detail 查询备份服务器使用情况
     * @param position 查询备份位置
     *
     * @return  List<BackupServerDTO>
     */
    List<BackupServerDTO> getBackupServer(String organId, String projectId, String clusterId, boolean detail, boolean position);

    /**
     * 移除项目备份服务器信息
     * @param organId 组织id
     * @param backupServerId 备份服务器id
     * @param clusterId 集群id
     *
     */
    void removeBackupServer(String organId, String projectId, Integer backupServerId, String clusterId);

}
