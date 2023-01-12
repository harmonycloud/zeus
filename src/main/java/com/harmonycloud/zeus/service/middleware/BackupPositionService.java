package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.BackupPositionDTO;
import com.harmonycloud.caas.common.model.BackupServerDTO;
import com.harmonycloud.zeus.bean.BeanBackupPosition;

import java.util.List;

/**
 * @author liyinlong
 * @since 2023/1/12 7:59 上午
 */
public interface BackupPositionService {

    List<BackupPositionDTO> selectBackupPositionDTOList(Integer backupServerId);

    List<BackupPositionDTO> selectBackupPositionDTOList(String projectId);

    List<BackupServerDTO> listBackupServerDTO(String projectId);

    List<BackupPositionDTO> list(String clusterId, String namespace);

    void create(BackupPositionDTO backupPositionDTO);

    void update(BackupPositionDTO backupPositionDTO);

    void delete(Integer id);

    BeanBackupPosition get(Integer backupServerId, String projectId);

}
