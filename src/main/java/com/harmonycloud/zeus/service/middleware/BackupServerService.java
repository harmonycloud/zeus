package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.BackupServerDTO;
import com.harmonycloud.zeus.bean.BeanBackupServer;

import java.util.List;
import java.util.Map;

/**
 * @author liyinlong
 * @since 2023/1/9 2:28 下午
 */
public interface BackupServerService {

    List<BackupServerDTO> list(String clusterId, String keyword);

    List<BackupServerDTO> listBackupPosition(String projectId);

    List<BackupServerDTO> listProjectEnableBackupServer(String projectId);

    BeanBackupServer get(Integer id);

    void create(BackupServerDTO backupServerDTO);

    void update(BackupServerDTO backupServerDTO);

    void allocate(Integer id, String clusterId);

    void delete(Integer id);

    Map<String,Integer> getBackupServerCountInfo();

    Integer getBackupServerCount(String clusterId);
}
