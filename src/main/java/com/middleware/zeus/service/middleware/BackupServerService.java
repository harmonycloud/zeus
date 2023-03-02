package com.middleware.zeus.service.middleware;

import com.middleware.caas.common.model.BackupServerDTO;
import com.middleware.zeus.bean.BeanBackupServer;

import java.util.List;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2023/1/9 2:28 下午
 */
public interface BackupServerService {

    List<BackupServerDTO> list(List<String> clusterIds, String keyword, Boolean withDetail);

    List<BackupServerDTO> listBackupPosition(String projectId);

    List<BackupServerDTO> listProjectBackupServer(String projectId);

    BeanBackupServer get(Integer id);

    void create(BackupServerDTO backupServerDTO);

    void update(BackupServerDTO backupServerDTO);

    void allocate(Integer id, String clusterId);

    void unbinding(String clusterId);

    void delete(Integer id);

    List<Map<String, String>> getBackupServerCountInfo();

    List<BeanBackupServer> listByClusterId(String clusterId);
}
