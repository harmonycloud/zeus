package com.middleware.zeus.service.middleware;

import com.middleware.caas.common.model.ProjectBackupServerDTO;
import com.middleware.zeus.bean.BeanProjectBackupServer;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/11 3:02 下午
 */
public interface ProjectBackupServerService {

    List<BeanProjectBackupServer> listByBackupServerId(Integer serverId);

    List<ProjectBackupServerDTO> listByProjectId(String projectId);

    void save(String projectId, List<Integer> backupServerIds);

    void deleteByProjectId(String projectId);

    void deleteByServerId(Integer serverId);

}
