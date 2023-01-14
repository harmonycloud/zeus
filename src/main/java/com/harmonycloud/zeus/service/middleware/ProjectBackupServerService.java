package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.ProjectBackupServerDTO;
import com.harmonycloud.zeus.bean.BeanProjectBackupServer;

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

}
