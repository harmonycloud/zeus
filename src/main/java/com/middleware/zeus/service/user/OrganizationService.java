package com.middleware.zeus.service.user;

import com.middleware.caas.common.model.BackupServerDTO;
import com.middleware.caas.common.model.ResourceQuotaDo;
import com.middleware.caas.common.model.user.OrganizationDto;
import com.middleware.caas.common.model.user.OrganizationQuota;
import com.middleware.caas.common.model.user.UserDto;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/3/7 2:09 下午
 */
public interface OrganizationService {

    /**
     * 新建组织
     * @param organizationDto 组织对象
     *
     */
    void add(OrganizationDto organizationDto);

    /**
     * 更新组织信息
     * @param organizationDto 组织对象
     *
     */
    void update(OrganizationDto organizationDto);

    /**
     * 查询组织列表
     * @param keyword 关键词
     *
     */
    List<OrganizationDto> list(String keyword);

    /**
     * 获取组织信息
     * @param organId 组织id
     *
     */
    OrganizationDto get(String organId);

    /**
     * 删除组织信息
     * @param organId 组织id
     *
     */
    void delete(String organId);

    /**
     * 资源分配
     * @param organizationQuota 组织资源对象
     *
     */
    void allocateQuota(OrganizationQuota organizationQuota);

    /**
     * 获取组织存储配额
     * @param organId 组织id
     * @param clusterId 集群id
     * @param detail 是否包含使用情况
     *
     * @return  List<ResourceQuotaDo>
     */
    List<ResourceQuotaDo> getStorageQuota(String organId, String clusterId, boolean detail);

    /**
     * 移除组织存储配额
     * @param organId 组织id
     * @param storageId 存储id
     * @param clusterId 集群id
     *
     */
    void removeStorageQuota(String organId, String storageId, String clusterId);

    /**
     * 获取组织cpu memory配额
     * @param organId 组织id
     * @param detail 是否包含使用情况
     *
     * @return  List<ResourceQuotaDo>
     */
    List<ResourceQuotaDo> getCpuMemoryQuota(String organId, boolean detail);

    /**
     * 移除组织cpu memory配额
     * @param organId 组织id
     * @param clusterId 集群id
     *
     */
    void removeCpuMemoryQuota(String organId, String clusterId);

    /**
     * 获取组织备份服务器信息
     * @param organId 组织id
     * @param clusterId 集群id
     * @param detail 查询备份服务器使用情况
     *
     * @return  List<BackupServerDTO>
     */
    List<BackupServerDTO> getBackupServer(String organId, String clusterId, boolean detail);

    /**
     * 移除组织备份服务器信息
     * @param organId 组织id
     * @param backupServerId 备份服务器id
     * @param clusterId 集群id
     *
     */
    void removeBackupServer(String organId, Integer backupServerId, String clusterId);

    /**
     * 获取组织用户信息
     * @param organId 组织id
     *
     */
    List<UserDto> listOrganUser(String organId, Boolean allocatable);

    /**
     * 获取组织用户信息
     * @param organizationDto 租户对象
     *
     */
    void addOrganUser(OrganizationDto organizationDto);

    /**
     * 获取组织用户信息
     * @param organId 组织id
     * @param username 用户名称
     * @param roleId 角色id
     *
     */
    void updateOrganUser(String organId, String username, Integer roleId);

    /**
     * 获取组织用户信息
     * @param organId 组织id
     * @param username 用户名称
     *
     */
    void deleteOrganUser(String organId, String username);

}
