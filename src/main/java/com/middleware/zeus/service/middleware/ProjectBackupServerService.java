package com.middleware.zeus.service.middleware;

import com.middleware.caas.common.model.ProjectBackupServerDTO;
import com.middleware.zeus.bean.BeanProjectBackupServer;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/11 3:02 下午
 */
public interface ProjectBackupServerService {

    /**
     * 查询备份服务器列表
     * @param organId 组织id
     * @param projectId 项目id
     *
     * @return List<ProjectBackupServerDTO>
     */
    List<ProjectBackupServerDTO> listByProjectId(String organId, String projectId);

    /**
     * 项目绑定备份服务器
     * @param organId 组织id
     * @param projectId 项目id
     * @param backupServerIds 备份服务器id列表
     *
     */
    void save(String organId, String projectId, List<Integer> backupServerIds);

    /**
     * 项目解绑备份服务器
     * @param organId 组织id
     * @param projectId 项目id
     *
     */
    void deleteByProjectId(String organId, String projectId);

    /**
     * 项目解绑备份服务器
     * @param organId 组织id
     * @param projectId 项目id
     * @param serverId 备份服务器id
     *
     */
    void delete(String organId, String projectId, Integer serverId);

}
